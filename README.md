# WordlTUI

A Wordl alternative to play whenever you want

## Build and publish

> [!NOTE]
> To push to the Github Registry, you need a PAT Token with at least `packages:write` permission. [Click here to obtain yours](https://github.com/settings/tokens/new) 

### Easy way 
1. Copy .env.example as .env and fill the personal access token obtained by github as well as your username 
2. run `chmod +x docker-deploy.sh && ./docker-deploy.sh` from the root of this repo

### Manual way
- Run the image build : `docker image build -t ghcr.io/[username]/wordltui-java-jar:latest .`
- Login to the docker registry : `docker login ghcr.io -u [username] --password-stdin [pat_token]`
- Push the image to the registry : `docker push ghcr.io/[username]/wordltui-java-jar:latest`

