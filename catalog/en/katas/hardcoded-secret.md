---
id: hardcoded-secret
---

## Why

A password in `application.properties` goes into the repository along with the code. Whoever has
access to the repository (the team, a former employee, a vendor, a leak) has access to the
database. And Git does not forget: deleting the line in a new commit does not take the password
out of the history.

The secret is also tied to the code. Changing it, after a leak or as routine, takes a commit and a
deploy. And the same value ends up serving every environment, because it is one file.

Spring and Quarkus solve this the same way: the file says **where** the value comes from
(`${DB_PASSWORD}`), and the environment says **what** it is. An environment variable, a Kubernetes
Secret, a vault such as Vault or AWS Secrets Manager.

## Acceptable when

In a development profile (`application-dev.properties`, `%dev.` in Quarkus), with the password of
the local database that `docker compose` starts. The Shishō does not flag those profiles. It does
not flag an empty value or a placeholder either, even with a default (`${DB_PASSWORD:local}`), but
the default is also read from the repository: keep there only what works for your machine.

The lesson never shows the value, so as not to spread the secret through the report and the CI log.

## Before

```properties
spring.datasource.url=jdbc:postgresql://db:5432/shop
spring.datasource.password=s3cr3t-db-p4ss
jwt.secret=nRvyYC4soFxBdZ
```

## After

The value comes from the environment: `DB_PASSWORD` and `JWT_SECRET`, set at deploy time, in a
Kubernetes Secret or in the `.env` that stays out of Git.

```properties
spring.datasource.url=jdbc:postgresql://db:5432/shop
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
```
