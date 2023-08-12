Currency Metagraph
===========

Sample project that runs a currency metagraph in a Constellation Network local cluster.

Forked and modified from the [Euclid Development Repository](https://github.com/Constellation-Labs/euclid-development-environment)

## Build
Make sure all the dependencies are installed according to the [Euclid Quickstart Guide](https://docs.constellationnetwork.io/sdk/guides/quick-start)

Add a `euclid.json` file in the root of the project that contains the following information (replace the info according to your needs):

```
{
  "github_token": "",
  "metagraph_id": "",
  "tessellation_version": "v2.0.0-alpha.10-data-api",
  "project_name": "currency-fire",
  "framework": {
    "name": "currency",
    "modules": [
      "data"
    ],
    "version": "v2.0.0-alpha.10-data-api",
    "ref_type": "branch"
  },
  "p12_files": {
    "genesis": {
      "file_name": "token-key.p12",
      "alias": "token-key",
      "password": "password"
    },
    "validators": [
      {
        "file_name": "token-key-1.p12",
        "alias": "token-key-1",
        "password": "password"
      },
      {
        "file_name": "token-key-2.p12",
        "alias": "token-key-2",
        "password": "password"
      } 
    ]
  },
  "docker": {
    "default_containers": [
      "global-l0",
      "metagraph-l0", 
      "metagraph-l1-currency", 
      "metagraph-l1-data", 
      "monitoring"
    ]
  }
}
```

Run the hydra build command to build the docker images
```
scripts/hydra build
```

## Run
```
scripts/hydra start_genesis
```
