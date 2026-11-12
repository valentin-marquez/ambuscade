# Releasing

Add a `## <version>` section to `CHANGELOG.md`, then push a tag:

```
git tag v0.1.0-mc1.21.1 && git push origin v0.1.0-mc1.21.1
```

The tag is `v<mod_version>-mc<minecraft_version>`, matching `gradle.properties`. A `-alpha.N` or `-beta.N`
suffix on the version (`v0.2.0-beta.1-mc1.21.1`) publishes as alpha or beta. The release workflow needs
a Modrinth token and the project id:

```
gh secret set MODRINTH_TOKEN --repo valentin-marquez/ambuscade
gh variable set MODRINTH_PROJECT_ID --repo valentin-marquez/ambuscade --body <project id or slug>
```
