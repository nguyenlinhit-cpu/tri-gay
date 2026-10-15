{
  description = "SupperMario Android development environment and APK export";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { nixpkgs, ... }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
        config.allowUnfree = true;
        config.android_sdk.accept_license = true;
      };
      jdk = pkgs.jdk25;
      buildToolsVersion = "37.0.0";
      sdk = (pkgs.androidenv.composeAndroidPackages {
        toolsVersion = null;
        platformVersions = [ "37.0" ];
        buildToolsVersions = [ buildToolsVersion ];
        includeEmulator = false;
        includeSystemImages = false;
        includeSources = false;
        includeNDK = false;
        includeCmake = false;
      }).androidsdk;
      androidHome = "${sdk}/libexec/android-sdk";
      setup = ''
        export JAVA_HOME="${jdk}"
        export ANDROID_HOME="${androidHome}"
        export ANDROID_SDK_ROOT="$ANDROID_HOME"
        export GRADLE_USER_HOME="$PWD/.gradle-home"
        export ANDROID_USER_HOME="$PWD/.android"
        export GRADLE_OPTS="''${GRADLE_OPTS:-} -Dorg.gradle.project.android.aapt2FromMavenOverride=${androidHome}/build-tools/${buildToolsVersion}/aapt2"
        mkdir -p "$GRADLE_USER_HOME" "$ANDROID_USER_HOME"
      '';
      buildApk = pkgs.writeShellApplication {
        name = "build-mario-apk";
        runtimeInputs = [ jdk pkgs.curl pkgs.unzip pkgs.coreutils ];
        text = ''
          if [[ ! -f app/build.gradle.kts || ! -f gradlew ]]; then
            echo "Run from the SupperMario project directory." >&2
            exit 1
          fi
          ${setup}
          bash ./gradlew --no-daemon -PlocalApk=true testDebugUnitTest lintRelease assembleRelease "$@"
          mkdir -p dist
          cp app/build/outputs/apk/release/app-release.apk dist/SupperMario.apk
          "${androidHome}/build-tools/${buildToolsVersion}/apksigner" verify --verbose dist/SupperMario.apk
          sha256sum dist/SupperMario.apk > dist/SupperMario.apk.sha256
          echo "APK: $PWD/dist/SupperMario.apk"
        '';
      };
    in {
      apps.${system}.default = {
        type = "app";
        program = "${buildApk}/bin/build-mario-apk";
        meta.description = "Test, optimize and export an installable local Android APK";
      };
      devShells.${system}.default = pkgs.mkShell {
        packages = [ jdk sdk pkgs.android-tools pkgs.curl pkgs.unzip ];
        shellHook = setup;
      };
    };
}
