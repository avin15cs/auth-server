# Repository instructions

- Use Java 21 and the Maven wrapper.
- Keep this service provider-neutral at its OIDC boundary. Google is the only upstream identity provider for now.
- Never commit OAuth client secrets, database credentials, signing private keys, tokens, or user data.
- Use Flyway for every database change; Hibernate must only validate schemas.
- OAuth/OIDC protocol behavior requires integration tests.
- Development targets `dev`; `main` is the production branch.

