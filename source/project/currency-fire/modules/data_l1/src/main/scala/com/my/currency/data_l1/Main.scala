package com.my.currency.data_l1

import java.util.UUID
import org.tessellation.BuildInfo
import org.tessellation.currency.l1.CurrencyL1App
import org.tessellation.schema.cluster.ClusterId

object Main
    extends CurrencyL1App(
      "currency-fire-data_l1",
      "currency-fire data L1 data node",
      ClusterId(UUID.fromString("517c3a05-9219-471b-a54c-21b7d72f4ae5")),
      version = BuildInfo.version
    ) {

}
