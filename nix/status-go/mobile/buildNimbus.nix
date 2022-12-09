{ pkgs, stdenv, lib, fetchFromGitHub
# Dependencies
, xcodeWrapper
, srcRaw
, writeScript
, writeTextFile
, androidPkgs
, git 
, nimCompiler 
, platform ? "android"
, arch ? "386"
, api ? "29" }:

let
  osId = builtins.elemAt (builtins.split "\-" stdenv.hostPlatform.system) 2;
  osArch = builtins.elemAt (builtins.split "\-" stdenv.hostPlatform.system) 0;

  
  # Arg arch -> Nim arch
  nimCpuMap = {
    "x86" = "i386";
    "x86-64" = "amd64"; 
    "arm" = "arm"; 
    "arm64" = "arm64";
  };

  iosArchMap = {
    "x86" = "x86_64";
    "x86-64" = "x86_64";
    "arm" = "armv7";
    "arm64" = "arm64";
  };

  nimCpu = if platform == "ios" && arch == "x86"
    then "amd64" else "${lib.getAttr arch nimCpuMap}";
  nimPlatform = "${(if platform == "ios" then "ios" else "android")}";

  iosSdk = if arch == "x86" then "iphonesimulator" else "iphoneos";
  iosArch = lib.getAttr arch iosArchMap;

  isAndroid = lib.hasPrefix "android" platform;
  isIOS = platform == "ios";


  buildType = lib.getConfig "build-type" "";


  routeHeader = builtins.readFile ./route.h;

  PROJECT_ROOT = srcRaw.src;

  # androidIncludes = stdenv.mkDerivation {
  #   name = "nim-status-android-includes";
  #   buildInputs = [ pkgs.coreutils pkgs.gnused ];
  #   builder = writeScript "nim-android-includes.sh"
  #   ''
  #     export PATH=${pkgs.coreutils}/bin:${pkgs.patch}/bin
  #     mkdir $out
  #     cd $out
  #     cp ${ANDROID_NDK_HOME}/sysroot/usr/include/stdio.h .
  #     patch -u stdio.h -i ${stdioPatch}
  #   '';
  # };

  iosIncludes = stdenv.mkDerivation {
    name = "nim-status-ios-includes";
    buildInputs = [ pkgs.coreutils ];
    builder = writeScript "nim-ios-includes.sh"
    ''
      export PATH=${pkgs.coreutils}/bin
      mkdir $out
      cd $out
      mkdir net
      echo "${routeHeader}" > net/route.h
    '';
  };


  ANDROID_NDK_HOME = "${androidPkgs.sdk}/ndk-bundle";
  
  
  androidToolchain = "${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/${osId}-${osArch}";
  androidTargetArchMap = {
    "x86" = "i686-linux-android";
    "x86-64" = "x86_64-linux-android";
    "arm" = "armv7a-linux-androideabi";
    "arm64" = "aarch64-linux-android";
  };

  androidTargetArch = lib.getAttr arch androidTargetArchMap;


  compilerFlags = if isAndroid then
    "--sysroot ${androidToolchain}/sysroot -fPIC -I${ANDROID_NDK_HOME}/sources/cxx-stl/llvm-libc++/include/ -I${PROJECT_ROOT}/vendor/nimbus-build-system/vendor/Nim-csources-v1/c_code -target ${androidTargetArch}${api} -I${androidToolchain}/sysroot/usr/include/${androidTargetArch}"
    else if isIOS then
    # TODO The conditional for -miphoneos-version-min=8.0 is required,
    # otherwise Nim will complain that thread-local storage is not supported for the current target
    # when expanding 'NIM_THREADVAR' macro
    "-isysroot $(xcrun --sdk ${iosSdk} --show-sdk-path) -I${PROJECT_ROOT}/vendor/nimbus-build-system/vendor/Nim-csources-v1/c_code -fembed-bitcode -arch ${iosArch} ${if arch == "arm" then "" else "-miphoneos-version-min=8.0"} -I${iosIncludes}"
    else throw "Unsupported platform!";

  linkerFlags = if isAndroid then
  "--sysroot ${androidToolchain}/sysroot -target ${androidTargetArch}${api}"
  else if isIOS then
  "--sysroot $(xcrun --sdk ${iosSdk} --show-sdk-path) -fembed-bitcode -arch ${iosArch}"
  else throw "Unsupported platform!";
  
  ldDirMap = {
    "x86" = "i686";
    "x86-64" = "x86_64";
    "arm" = "armeabi-v7a"; 
    "arm64" = "arm64-v8a";
  };

  cmakeArgs = if isAndroid then
    "-DCMAKE_SYSTEM_NAME=Android -DCMAKE_SYSTEM_VERSION=${api} -DCMAKE_ANDROID_ARCH_ABI=${lib.getAttr arch ldDirMap} -DCMAKE_ANDROID_NDK=${ANDROID_NDK_HOME}"
  else if isIOS then ""
  else throw "Unsupported platform!";
  #clangPath = if isAndroid then "${androidToolchain}/bin" else "";
  #clangName = if isAndroid then "${androidTargetArch}${api}-clang" else "";

  compilerVars = if isAndroid then
    ''
      export PATH=${androidToolchain}/bin:$PATH
      export CC=clang
      export CXX=clang++
      export AR=${androidToolchain}/bin/llvm-ar
      export NM=${androidToolchain}/bin/llvm-nm
      export RANLIB=${androidToolchain}/bin/llvm-ranlib
      export RANLIB=${androidToolchain}/bin/llvm-ranlib
      export LD=${androidToolchain}/bin/ld
      export AS=$CC
      export CFLAGS="${compilerFlags}"
      export LDFLAGS="${linkerFlags}"

      # This is important, otherwise Nim might not use proper tooling
      mkdir bin
      ln -s $AR bin/ar
      ln -s $AS bin/as
      #ln -s $CC bin/gcc
      #ln -s $CC bin/clang
      ln -s $RANLIB bin/ranlib

      touch bin/git
      chmod +x bin/git
      export PATH=./bin:$PATH
    ''
    else if isIOS then
    ''
      mkdir bin
      touch bin/git
      chmod +x bin/git
      export PATH=`pwd`/bin:$PATH

      export PATH=${xcodeWrapper}/bin:$PATH
      export CC=$(xcrun --sdk ${iosSdk} --find clang)
      export CXX=$(xcrun --sdk ${iosSdk} --find clang++)
      export CMAKE_CXX_COMPILER=$(xcrun --sdk ${iosSdk} --find clang++)
      export CFLAGS="${compilerFlags}"
      export LDFLAGS="${linkerFlags}"
    ''
    else throw "Unsupported platform!";
 
  # src = pkgs.fetchgit {
  #   url = "https://github.com/status-im/nim-status";
  #   rev = "33241ee463291ed912681eff278658dc1dba1dcb";
  #   sha256 = "1ki32s13zrwjzhbdyzfzj29mj3hkib2iwqwcwvlan9699p4v19iq";
  #   leaveDotGit = true;
  #   fetchSubmodules = false;
  # };

  src = srcRaw;


  nimHostOs = if osId == "darwin" then "Darwin"
              else if osId == "linux" then "Linux"
              else "Windows_NT";
  createNimbleLink1 = writeTextFile {
    name = "createNimbleLink.sh";
    text = ''
      export EXCLUDED_NIM_PACKAGES=""
      export NIMBLE_LINK_SCRIPT=$PWD/vendor/nimbus-build-system/scripts/create_nimble_link.sh
      export NIMBLE_DIR=$PWD/vendor/.nimble
      export PWD_CMD=$(which pwd)
      patchShebangs scripts > /dev/null
      patchShebangs $PWD/vendor/nimbus-build-system/scripts > /dev/null
      for dep_dir in $(find vendor -type d -maxdepth 1); do
          pushd "$dep_dir" >/dev/null
          $NIMBLE_LINK_SCRIPT "$dep_dir"
          popd >/dev/null
      done
    '';
    executable = true;
  };

  createNimbleLink = writeTextFile {
    name = "createNimbleLink.sh";
    text = ''
    mkdir -p vendor/.nimble/pkgs

    curdir=`pwd`
    for dir in vendor/*/;
    do
      baseDirName=`basename ''${dir}`
      dirName=vendor/.nimble/pkgs/$baseDirName-\#head
      echo $dirName
      mkdir -p $dirName

      packageDir="''${curdir}/vendor/''${baseDirName}"
      if [ -d "''${packageDir}/src" ]; then
        packageDir="''${packageDir}/src"
      fi
      echo "''${packageDir}" > ''${dirName}/''${baseDirName}.nimble-link
      echo "''${packageDir}" >> ''${dirName}/''${baseDirName}.nimble-link
    done
    '';
    executable = true;
  };

in stdenv.mkDerivation rec {
  pname = "liblcproxy";
  name = "liblcproxy";
  src = srcRaw.src;
  #version = lib.strings.substring 0 7 src.rev;
  buildInputs = with pkgs; [ wget git which tcl cmake libtool];

  phases = [ "unpackPhase" "preBuildPhase" "buildPhase" "installPhase" ];

  preBuildPhase = ''
    #sed -E -i 's|^(.*)(-C vendor/nim-nat-traversal/vendor/miniupnp/miniupnpc)(.*)|\1 \2 CFLAGS="$(CFLAGS)" CC="$(CC)"\3|g' vendor/nimbus-build-system/makefiles/targets.mk
    sed -E -i 's|^(LIBBACKTRACE_SED := true.*)|\1\nCMAKE_ARGS := ${cmakeArgs}|g' vendor/nim-libbacktrace/Makefile
    #sed -E -i 's|^(.*)(-C vendor/nim-libbacktrace --no-print-directory BUILD_CXX_LIB=0)|\1 \2 CFLAGS="$(CFLAGS)"|g' Makefile
    #sed -E -i 's|^CC :=.*|CC := $(CC)|g' vendor/nim-libbacktrace/Makefile
    #sed -E -i 's|^CXX :=.*|CXX := $(CXX)|g' vendor/nim-libbacktrace/Makefile
    #sed -E -i 's|--host=arm| |g' vendor/nim-libbacktrace/Makefile
    #sed -E -i 's|--build=\$\(\./config.guess\)| |g' vendor/nim-libbacktrace/Makefile
    #sed -E -i 's|^(.*\./configure --prefix="/usr")(.*)|\1 --host=${androidTargetArch} --target=${androidTargetArch} CC="$(CC)" \2|g' vendor/nim-libbacktrace/Makefile
    sed -E -i 's|^(.*)(useNews\* = )(.*)|\1\2 false|g' vendor/nim-json-rpc/json_rpc/clients/config.nim



    export HOME=$PWD
    # echo 'switch("passC", "${compilerFlags}")' >> config.nims
    # echo 'switch("passL", "${linkerFlags}")' >> config.nims
    # echo 'switch("cpu", "${nimCpu}")' >> config.nims
    # echo 'switch("os", "${nimPlatform}")' >> config.nims
    # echo 'switch("cc", "clang")' >> config.nims

    # echo 'switch("${nimCpu}.${nimPlatform}.clang.path", "{clangPath}")' >> config.nims
    # echo 'switch("${nimCpu}.${nimPlatform}.clang.exe", "{clangName}")' >> config.nims
    # echo 'switch("${nimCpu}.${nimPlatform}.clang.linkerexe", "{clangName}")' >> config.nims

    # echo 'switch("passC", "${compilerFlags}")' >> vendor/nimbus-eth2/config.nims
    # echo 'switch("passL", "${linkerFlags}")' >> vendor/nimbus-eth2/config.nims
    # echo 'switch("cpu", "${nimCpu}")' >> vendor/nimbus-eth2/config.nims
    # echo 'switch("os", "${nimPlatform}")' >> vendor/nimbus-eth2/config.nims
    # echo 'switch("cc", "clang")' >> vendor/nimbus-eth2/config.nims
    # echo 'switch("${nimCpu}.${nimPlatform}.clang.path", "{clangPath}")' >> vendor/nimbus-eth2/config.nims
    # echo 'switch("${nimCpu}.${nimPlatform}.clang.exe", "{clangName}")' >> vendor/nimbus-eth2/config.nims
    # echo 'switch("${nimCpu}.${nimPlatform}.clang.linkerexe", "{clangName}")' >> vendor/nimbus-eth2/config.nims

    # echo 'switch("passC", "${compilerFlags}")' >> vendor/nim-sqlite3-abi/config.nims
    # echo 'switch("passL", "${linkerFlags}")' >> vendor/nim-sqlite3-abi/config.nims
    # echo 'switch("cpu", "${nimCpu}")' >> vendor/nim-sqlite3-abi/config.nims
    # echo 'switch("os", "${nimPlatform}")' >> vendor/nim-sqlite3-abi/config.nims
    # echo 'switch("cc", "clang")' >> vendor/nim-sqlite3-abi/config.nims
    # echo 'switch("${nimCpu}.${nimPlatform}.clang.path", "{clangPath}")' >> vendor/nim-sqlite3-abi/config.nims
    # echo 'switch("${nimCpu}.${nimPlatform}.clang.exe", "{clangName}")' >> vendor/nim-sqlite3-abi/config.nims
    # echo 'switch("${nimCpu}.${nimPlatform}.clang.linkerexe", "{clangName}")' >> vendor/nim-sqlite3-abi/config.nims


    ${createNimbleLink}

    ${compilerVars}
    export PATH=${nimCompiler}/bin:$PATH
    which clang
    echo $PATH
  '';


  buildPhase = ''
    make V=3 OS=${nimHostOs} \
      CC=clang USE_SYSTEM_NIM=1 \
      NIMFLAGS="\
      --passC:\"${compilerFlags}\" \
      --passL:\"${linkerFlags}\" \
      --cc:clang \
      --cpu:${nimCpu} \
      --os:${nimPlatform} \
      -d:disableMarchNative" \
    liblcproxy

   '';

  installPhase = ''
    mkdir -p $out
    cp build/liblcproxy/liblcproxy* $out/
    cp build/liblcproxy/lcproxy.h $out/
    cp build/liblcproxy/nimbase.h $out/
  '';
}
