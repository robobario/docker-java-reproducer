# docker-java-reproducer
reproduce podman issue

Run: 
```
mvn test
```

Or if you are on OSX or want to point it at a different socket:

```
DOCKER_URI=unix:///var/run/docker.sock mvn test
```
