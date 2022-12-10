# Proxy-System

This is an all-in-one proxy-system for [Velocity](https://github.com/PaperMC/Velocity) and [SimpleCloud](https://github.com/theSimpleCloud/SimpleCloud)

## Installation

Firstly, download the [source](https://github.com/PhilipKrauss/proxy-system/archive/refs/heads/master.zip) or [clone](https://github.com/PhilipKrauss/proxy-system.git) the project into your ide.

Then change your mysql-database credentials inside the [database/DatabaseAdapter](https://github.com/PhilipKrauss/proxy-system/tree/master/src/main/java/it/philipkrauss/proxysystem/database/DatabaseAdapter.java#L23) (A working connection is required for the plugin to run):

```java
DatabaseCredentials credentials = DatabaseCredentials.create();
credentials.setHostname("localhost");
credentials.setPort(3306);
credentials.setDatabase("database");
credentials.setUsername("username");
credentials.setPassword("password");
```

After changing those credentials you'll have to compile and build this plugin with [Maven](https://maven.apache.org/)

Now copy the generated jar-File into the plugins-folder of your proxy (e.g. /templates/Proxy/plugins/)

Then restart your proxy by executing `shutdowngroup <your-proxy-group>`

## Features

These are the major features that this proxy-system does have already (more coming soon)
- punish-system
- team-login and team-chat
- friend-system
- player-info

## Contributing

Feel free to make change the code for your needs. You're welcome to create pull requests to contribute to this project. For major changes please open an issue first to discuss what you'd like to change :)

## License

Copyright © 2022 Philip Krauß