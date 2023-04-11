Currency Metagraph
===========

Sample project that runs a currency metagraph in a Constellation Network local cluster.

Forked and modified from the [Euclid Development Repository](https://github.com/Constellation-Labs/euclid-development-environment)

## Build
Make sure all the dependencies are installed according to the [Euclid Quickstart Guide](https://docs.constellationnetwork.io/sdk/guides/quick-start)

Add a `.env` file in the root of the project that contains the following information (replace the info according to your needs):

```
GITHUB_TOKEN=

PROJECT_NAME=currency-fire

DEFAULT_TESSELLATION_PROJECT_VERSION=v2.0.0-alpha.2
DEFAULT_TESSELLATION_PROJECT_VERSION_TEMPLATE=2.0.0-alpha.2

P12_GENESIS_FILE_NAME=token-key.p12
P12_GENESIS_FILE_KEY_ALIAS=token-key
P12_GENESIS_FILE_PASSWORD=password

P12_NODE_2_FILE_NAME=token-key-1.p12
P12_NODE_2_FILE_KEY_ALIAS=token-key-1
P12_NODE_2_FILE_PASSWORD=password

P12_NODE_3_FILE_NAME=token-key-2.p12
P12_NODE_3_FILE_KEY_ALIAS=token-key-2
P12_NODE_3_FILE_PASSWORD=password
```

Run the hydra build command to build the docker images
```
scripts/hydra build
```

## Run
```
scripts/hydra start_genesis
```
