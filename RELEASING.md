# Releasing

 1. Update `POM_VERSION_NAME` to the new version `X.Y.Z` in `permission/gradle.properties`.
 2. Update version name in `README.md`
 3. Add change log in `CHANGELOG.md`
 4. Execute `git commit -m "Prepare for release X.Y.Z"` (where X.Y.Z is the new version).
 5. Execute `./gradlew clean bintrayUpload`.
 6. Execute `git tag v_X.Y.Z"` (where X.Y.Z is the new version)
 7. Execute `git push && git push --tags`

