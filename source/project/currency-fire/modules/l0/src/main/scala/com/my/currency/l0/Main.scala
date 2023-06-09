package com.my.currency.l0

import java.util.UUID

import cats.effect.{IO, Resource}
import cats.syntax.semigroupk._

import org.tessellation.BuildInfo
import org.tessellation.currency.l0.cli.method
import org.tessellation.currency.l0.cli.method.{Run, RunGenesis, RunValidator}
import org.tessellation.currency.l0.http.P2PClient
import org.tessellation.currency.l0.modules._
import org.tessellation.currency.schema.currency.CurrencySnapshot
import org.tessellation.ext.cats.effect.ResourceIO
import org.tessellation.ext.kryo._
import org.tessellation.schema.cluster.ClusterId
import org.tessellation.schema.node.NodeState
import org.tessellation.sdk.app.{SDK, TessellationIOApp}
import org.tessellation.sdk.infrastructure.genesis.{Loader => GenesisLoader}
import org.tessellation.sdk.infrastructure.gossip.{GossipDaemon, RumorHandlers}
import org.tessellation.sdk.resources.MkHttpServer
import org.tessellation.sdk.resources.MkHttpServer.ServerName
import org.tessellation.sdk.{SdkOrSharedOrKernelRegistrationIdRange, sdkKryoRegistrar}

import com.monovore.decline.Opts

object Main
    extends TessellationIOApp[Run](
      "currency-fire-l0",
      "currency-fire L0 node",
      ClusterId(UUID.fromString("517c3a05-9219-471b-a54c-21b7d72f4ae5")),
      version = BuildInfo.version
    ) {

  val opts: Opts[Run] = method.opts

  type KryoRegistrationIdRange = SdkOrSharedOrKernelRegistrationIdRange

  val kryoRegistrar: Map[Class[_], KryoRegistrationId[KryoRegistrationIdRange]] =
    sdkKryoRegistrar

  def run(method: Run, sdk: SDK[IO]): Resource[IO, Unit] = {
    import sdk._

    val cfg = method.appConfig

    for {
      _ <- Resource.unit
      p2pClient = P2PClient.make[IO](sdkP2PClient, sdkResources.client, keyPair, method.identifier)
      queues <- Queues.make[IO](sdkQueues).asResource
      storages <- Storages.make[IO](sdkStorages, cfg.snapshot, method.globalL0Peer).asResource
      validators = Validators.make[IO](seedlist)
      services <- Services
        .make[IO](
          p2pClient,
          sdkServices,
          storages,
          validators,
          sdkResources.client,
          sdkServices.session,
          sdk.seedlist,
          sdk.nodeId,
          keyPair,
          cfg,
          method.identifier
        )
        .asResource
      programs = Programs.make[IO](sdkPrograms, storages, services, p2pClient)
      healthChecks <- HealthChecks
        .make[IO](
          storages,
          services,
          programs,
          p2pClient,
          sdkResources.client,
          sdkServices.session,
          cfg.healthCheck,
          sdk.nodeId
        )
        .asResource
      rumorHandler = RumorHandlers.make[IO](storages.cluster, healthChecks.ping, services.localHealthcheck).handlers <+>
        services.consensus.handler
      _ <- Daemons
        .start(storages, services, programs, queues, healthChecks)
        .asResource
      api = HttpApi
        .make[IO](
          storages,
          queues,
          services,
          programs,
          healthChecks,
          keyPair.getPrivate,
          cfg.environment,
          sdk.nodeId,
          BuildInfo.version,
          cfg.http
        )
      _ <- MkHttpServer[IO].newEmber(ServerName("public"), cfg.http.publicHttp, api.publicApp)
      _ <- MkHttpServer[IO].newEmber(ServerName("p2p"), cfg.http.p2pHttp, api.p2pApp)
      _ <- MkHttpServer[IO].newEmber(ServerName("cli"), cfg.http.cliHttp, api.cliApp)

      gossipDaemon = GossipDaemon.make[IO](
        storages.rumor,
        queues.rumor,
        storages.cluster,
        p2pClient.gossip,
        rumorHandler,
        validators.rumorValidator,
        services.localHealthcheck,
        nodeId,
        generation,
        cfg.gossip.daemon,
        services.collateral
      )

      _ <- (method match {
        case _: RunValidator =>
          gossipDaemon.startAsRegularValidator >>
            programs.globalL0PeerDiscovery.discoverFrom(cfg.globalL0Peer) >>
            storages.node.tryModifyState(NodeState.Initial, NodeState.ReadyToJoin)
        case m: RunGenesis =>
          storages.node.tryModifyState(
            NodeState.Initial,
            NodeState.LoadingGenesis,
            NodeState.GenesisReady
          ) {
            GenesisLoader
              .make[IO]
              .load(m.genesisPath)
              .flatMap { accounts =>
                val genesis = CurrencySnapshot.mkGenesis(
                  accounts.map(a => (a.address, a.balance)).toMap
                )
                services.genesis.accept(genesis)
              }
          } >>
            gossipDaemon.startAsInitialValidator >>
            services.cluster.createSession >>
            services.session.createSession >>
            programs.globalL0PeerDiscovery.discoverFrom(cfg.globalL0Peer) >>
            storages.node.setNodeState(NodeState.Ready)
      }).asResource
    } yield ()
  }
}
