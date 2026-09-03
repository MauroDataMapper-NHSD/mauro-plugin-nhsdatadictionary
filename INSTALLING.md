# Installation instructions

Instructions to install Mauro and this plugin on a fresh virtual machine
With these instructions, the develop branch of the code is checked out and built locally.  The process is 
much simpler if you intend to just use 'release' packages of Mauro from Docker.

## 1. Configure Virtual Machine

### Software installation

1. Update package list and upgrade packages:

```aiignore
sudo apt-get update
```

```aiignore
sudo apt-get upgrade
```


2. Install a text editor of choice - e.g. emacs:

```aiignore
sudo apt-get install emacs
```

3. Install docker.  Follow the instructions here:
[Install Docker Engine on Ubuntu](https://docs.docker.com/engine/install/ubuntu/)
Use the instructions at "Install using the apt repository".

The add the user to the docker group:

```aiignore
sudo usermod -aG docker <user>
```

(And log out, log in again to pick up the group membership)
The user should now be able to run docker commands without sudo.

4. Install xml command line tools (including xmllint, which is used in the update-plugin.sh script):

```aiignore
sudo apt install libxml2-utils
```

5. Install java, for building Mauro locally:
   
```aiignore
sudo apt install default-jre
```

### Download Mauro and install

1. Create some appropriate directories

```aiignore
cd /data/
mkdir postgresql backups

cd /opt
sudo mkdir mauro mauro/code mauro/bin mauro/init mauro/init/micronaut mauro/logs mauro/orchestration 

cd /opt/mauro
ln -s /data/postgresql postgresql
ln -s /data/backups backups

sudo chown -R <user> /opt/mauro
```

2. Download Mauro codebase

```aiignore
cd /opt/mauro/code
git clone https://github.com/MauroDataMapper/mauro-micronaut.git
```

3. Setup the scripts for controlling Mauro

```aiignore
cd /opt/mauro/bin
```

a. `backup-postgres.sh`
```aiignore
pushd /opt/mauro/backups/postgres/
docker exec mauro-micronaut pg_dump -U postgres sandbox | gzip -9  > db-backup-$(date +%y-%m-%d).sql.gz
popd
```

b. `docker-build.sh`
```aiignore
pushd /opt/mauro/code/mauro-micronaut
./gradlew dockerBuild
popd
```

c. `docker-exec.sh`
```aiignore
docker exec -it mauro-micronaut bash
```

d. `get-temp-password.sh`
```aiignore
#!/bin/bash
docker exec mauro-micronaut psql -U postgres -d sandbox -c "SELECT temp_password  FROM security.catalogue_user WHERE email_address like '$1';"
```

e. `start-docker.sh`
```aiignore
VERSION=amd64-0.0.4-SNAPSHOT
docker run --rm -d \
       --name mauro-micronaut \
       --cap-drop=ALL \
       --cap-add=NET_BIND_SERVICE \
       --cap-add=SETUID \
       --cap-add=SETGID \
       --cap-add=CHOWN \
       -p 8080:8080 \
       -v /opt/mauro/init:/opt/init:ro \
       -v /opt/mauro/postgresql:/var/lib/postgresql/data \
       -v /opt/mauro/logs:/var/logs \
       -v /opt/mauro/orchestration/:/home/app/resources/public/orchestration \
       maurodatamapper/mauro:$VERSION
```

f. `stop-docker.sh`
```aiignore
docker stop mauro-micronaut
```

g. `update-mauro-micronaut.sh`
```aiignore
pushd /opt/mauro/code/mauro-micronaut
git fetch
git checkout develop
git pull
popd
```

h. `update-mauro-orchestration.sh`
```aiignore
#!/usr/bin/env bash

# This ensures the script fails if the wget fails
set -euo pipefail 

VERSION=1.0.0-SNAPSHOT

pushd /opt/mauro/orchestration
rm -r /opt/mauro/orchestration/*
wget https://mauro-repository.com/artifacts-snapshots/mauroDataMapper/nhsd-datadictionary-orchestration/nhsd-datadictionary-orchestration-$VERSION.tgz
tar -xvzf nhsd-datadictionary-orchestration-$VERSION.tgz
mv nhsd-datadictionary-orchestration-$VERSION/* .
rm nhsd-datadictionary-orchestration-$VERSION.tgz
rm -rf nhsd-datadictionary-orchestration-$VERSION

# Repalce the default api endpoint
sed -i 's|/nhsd-datadictionary/api|/api|g' main*.js

popd
```

i. `update-plugin.sh`
```aiignore
VERSION=0.0.1-SNAPSHOT
BASE_URL="https://mauro-repository.com/libs-snapshot-local/org/maurodata/plugins/mauro-plugin-nhsdatadictionary/$VERSION"

pushd /opt/mauro/init/micronaut

VALUE=$(wget -qO- "$BASE_URL/maven-metadata.xml" | xmllint --xpath 'string(/metadata/versioning/snapshotVersions/snapshotVersion[classifier="all" and extension="jar"]/value)' -)

JAR="mauro-plugin-nhsdatadictionary-${VALUE}-all.jar"
rm mauro-plugin-nhsdatadictionary*.jar
wget -q --show-progress -O $JAR "$BASE_URL/$JAR" || { echo "wget failed" >&2; exit 1; }

popd
```

4. Make the scripts executable:

```aiignore
chmod a+x *.sh
```

## Configure Mauro

1. Edit the file `/opt/mauro/init/micronaut/application-mauro.yml` and use the following content:

```aiignore
micronaut:
  server:
#    forwarded-headers:
#      enabled: true
    cors:
      enabled: true
      configurations:
        ui:
          allowed-origins:
            - <url - e.g. https://mauro.uat.dataproducts.nhs.uk>
  security:
    endpoints:
      oauth:
        enabled: true
    # Populate to enable OAuth2; see also mauro.oauth properties
    oauth2:
      callback-uri: <url - e.g. https://mauro.uat.dataproducts.nhs.uk/oauth/callback{/provider}>
      clients:
        microsoft:
          client-id: <client-id>
          client-secret: <client-secret>
          openid:
            issuer: <issuer>
            authorization:
              prompt: login
    redirect:
      login-success: <url - e.g. https://mauro.uat.dataproducts.nhs.uk/redirect/redirects/open-id-connect-redirect.html>
#      login-failure: https://mauro.uat.dataproducts.nhs.uk/#/auth/finalize/sign-in-failed

mauro:
  users:
    -   email: admin@maurodatamapper.com
        first-name: admin
        last-name: admin
        temp-password: a_password
  groups:
    -   name: Administrators
        is-admin: true
        members:
          - admin@maurodatamapper.com
  api-keys:
    -   name: My first API Key
        email: admin@maurodatamapper.com
        refreshable: true
        expiry: 2027-12-31

# Populate to enable OAuth2; see also micronaut.security properties
  oauth:
    id: 00000000-0000-0000-0000-000000000001
    label: NHS Single Sign-on
    standard-provider: true
    authorization-endpoint: <url - e.g. https://mauro.uat.dataproducts.nhs.uk/oauth/login/microsoft>
    image-url: <url - e.g. https://www.england.nhs.uk/nhsidentity/wp-content/themes/nhsengland-identity/templates/assets/img/global/nhs-logo.svg>
    login-success: <url - e.g. https://mauro.uat.dataproducts.nhs.uk/redirects/open-id-connect-redirect.html>
    require-verified-email: false
    create-user: true

logger:
  levels:
    io.micronaut.context.condition: DEBUG
```

2. Add a `change-theme.sh` script to use the NHS theme:

```aiignore
echo "Changing to use the nhs-digital user interface theme..."
pushd /home/app/resources/public
sed -i 's|themeName:"default"|themeName:"nhs-digital"|g' main*.js
popd
```

```aiignore
chmod a+x change-theme.sh
```

## Configure nginx

1. Edit the file `/etc/nginx/sites-available/default` and replace the `location /` block with:

```aiignore
        location / {
                proxy_pass http://127.0.0.1:8080;

                proxy_connect_timeout 600;
                proxy_send_timeout 600;
                proxy_read_timeout 600;
                send_timeout 600;
        }
```

2. Restart nginx:

```
sudo service nginx restart
```

3. Install an SSL certificate from LetsEncrypt:

Follow the instructions here:
[[Install LetsEncrypt on Ubuntu 18.04](https://certbot.eff.org/lets-encrypt/ubuntubionic-nginx)](https://www.digitalocean.com/community/tutorials/how-to-secure-nginx-with-let-s-encrypt-on-ubuntu-20-04)