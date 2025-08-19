# Requirements
- java 21+
- docker
- openssl (to create cert)


# openssl
1. create key.pem
```
openssl genrsa -out key.pem 2048 
```
2. create cert.pem
```
openssl req -new -x509 -key key.pem -out cert.pem -days 365 -subj "/CN=localhost"
```
3. move them to the `/src/main/recources`

# run
## via IntellijIdea
1. open project
2. run `gradle clean bootRun`

`bootRun` is a self-registered task and could be found in build.gradle file

## via docker
in project root dir run: `docker-compose up --build`

# testing
(using -vk to avoid cert issues)
## healthcheck
`curl -vk -L 'https://localhost:8081/health'`
## metrics
`curl -vk -L 'https://localhost:8081/metrics'`
## functional requests
request:

`curl -vk -L 'https://localhost:8081/' --header 'Content-Type: application/json' --data '{"jsonrpc": "2.0","method": "eth_blockNumber","params": [],"id": 1}'`

response:

`{"jsonrpc":"2.0","result":"0x1614b1d","id":1}`

request:

`curl -vk 'https://localhost:8081/' -H 'Content-Type: application/json' -d '[{"jsonrpc":"2.0","method":"eth_getBlockByNumber","params":[],"id":1},{"jsonrpc":"2.0","method":"eth_chainId","params":[],"id":2}]'`

response:

`[{"jsonrpc":"2.0","id":1,"error":{"code":-32602,"message":"missing value for required argument 0"}},{"jsonrpc":"2.0","id":2,"result":"0x1"}]`

request:

`curl -vk 'https://localhost:8081' -H 'Content-Type: application/json' -d '{"jsonrpc":"2.0","method":"eth_getLogs","params":[{"fromBlock":"0x1554e99","toBlock":"0x1554f99"}],"id":74}'`

response:

`{"jsonrpc":"2.0","error":{"code":-32701,"message":"Please specify an address in your request or, to remove restrictions, order a dedicated full node here: https://www.allnodes.com/eth/host"},"id":74}`
