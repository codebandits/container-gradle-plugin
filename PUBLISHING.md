# Publishing Guide

## Verify Signing Key Expiration

Before publishing a new release, ensure that the GPG signing key has not expired. This check is not automated in the build process.

To manually check the expiration date of the signing key:

1. Import the project signing key into your GPG keyring.

    ```sh
    echo "<signing.key string from signing.enc.properties>" | base64 --decode | gpg --import
    ```

2. List the keys in your keyring to view expiration details.

    ```sh
    gpg --list-keys
    ```

3. If the key is expired or nearing expiration, follow the instructions in the `signing.enc.properties` file to extend the key's expiration.

## Publish a Release

Once the changelog is ready, run the publish GitHub Actions workflow.
