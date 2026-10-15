#!/usr/bin/env python3
"""Create the checked-in, dependency-free Xcode project deterministically."""
import hashlib
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
def ident(name): return hashlib.sha256(name.encode()).hexdigest()[:24].upper()
objects = []
def add(key, value): objects.append(f'\t\t{ident(key)} = {{ {value} }};'); return ident(key)
files = []
for folder in ['App', 'GameCore/Sources/GameCore']:
    children = []
    for path in sorted((ROOT / 'ios' / folder).glob('*.swift')):
        key = f'file:{folder}/{path.name}'
        fid = add(key, f'isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = "{path.name}"; sourceTree = "<group>";')
        files.append(add('build:'+key, f'isa = PBXBuildFile; fileRef = {fid};'))
        children.append(fid)
    add('group:'+folder, f'isa = PBXGroup; path = "{folder}"; sourceTree = "<group>"; children = ({", ".join(children)});')
resource = add('resource', 'isa = PBXFileReference; lastKnownFileType = folder; path = Resources; sourceTree = "<group>";')
resource_build = add('resourceBuild', f'isa = PBXBuildFile; fileRef = {resource};')
assets = add('assets', 'isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; path = Assets.xcassets; sourceTree = "<group>";')
assets_build = add('assetsBuild', f'isa = PBXBuildFile; fileRef = {assets};')
product = add('product', 'isa = PBXFileReference; explicitFileType = wrapper.application; path = SupperMario.app; sourceTree = BUILT_PRODUCTS_DIR;')
uitest_product = add('uitestProduct', 'isa = PBXFileReference; explicitFileType = wrapper.cfbundle; path = NativeGameUITests.xctest; sourceTree = BUILT_PRODUCTS_DIR;')
uitest_file = add('uitestFile', 'isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = NativeGameUITests.swift; sourceTree = "<group>";')
uitest_group = add('uitestGroup', f'isa = PBXGroup; path = UITests; sourceTree = "<group>"; children = ({uitest_file});')
uitest_build = add('uitestBuild', f'isa = PBXBuildFile; fileRef = {uitest_file};')
uitest_sources = add('uitestSources', f'isa = PBXSourcesBuildPhase; buildActionMask = 2147483647; files = ({uitest_build}); runOnlyForDeploymentPostprocessing = 0;')
uitest_frameworks = add('uitestFrameworks', 'isa = PBXFrameworksBuildPhase; buildActionMask = 2147483647; files = (); runOnlyForDeploymentPostprocessing = 0;')
proxy = add('uitestProxy', f'isa = PBXContainerItemProxy; containerPortal = {ident("project")}; proxyType = 1; remoteGlobalIDString = {ident("target")}; remoteInfo = SupperMario;')
dependency = add('uitestDependency', f'isa = PBXTargetDependency; target = {ident("target")}; targetProxy = {proxy};')
uitest_configs = []
for mode in ['Debug', 'Release']:
    uitest_configs.append(add('uitestConfig:'+mode, f'''isa = XCBuildConfiguration; name = {mode}; buildSettings = {{
    PRODUCT_NAME = "$(TARGET_NAME)"; PRODUCT_BUNDLE_IDENTIFIER = vn.local.suppermario.UITests; GENERATE_INFOPLIST_FILE = YES;
    IPHONEOS_DEPLOYMENT_TARGET = 15.0; SWIFT_VERSION = 5.0; SDKROOT = iphoneos; TARGETED_DEVICE_FAMILY = "1,2";
    TEST_TARGET_NAME = SupperMario; CODE_SIGN_STYLE = Automatic; DEVELOPMENT_TEAM = "";
    LD_RUNPATH_SEARCH_PATHS = "$(inherited) @executable_path/Frameworks @loader_path/Frameworks";
    }};'''))
uitest_config_list = add('uitestConfigs', f'isa = XCConfigurationList; buildConfigurations = ({", ".join(uitest_configs)}); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release;')
add('uitestTarget', f'''isa = PBXNativeTarget; name = NativeGameUITests; productName = NativeGameUITests;
productType = "com.apple.product-type.bundle.ui-testing"; productReference = {uitest_product}; buildConfigurationList = {uitest_config_list};
buildPhases = ({uitest_sources}, {uitest_frameworks}); buildRules = (); dependencies = ({dependency});''')
products = add('products', f'isa = PBXGroup; name = Products; sourceTree = "<group>"; children = ({product}, {uitest_product});')
add('mainGroup', f'isa = PBXGroup; sourceTree = "<group>"; children = ({ident("group:App")}, {ident("group:GameCore/Sources/GameCore")}, {resource}, {assets}, {uitest_group}, {products});')
sources = add('sources', f'isa = PBXSourcesBuildPhase; buildActionMask = 2147483647; files = ({", ".join(files)}); runOnlyForDeploymentPostprocessing = 0;')
resources = add('resources', f'isa = PBXResourcesBuildPhase; buildActionMask = 2147483647; files = ({resource_build}, {assets_build}); runOnlyForDeploymentPostprocessing = 0;')
frameworks = add('frameworks', 'isa = PBXFrameworksBuildPhase; buildActionMask = 2147483647; files = (); runOnlyForDeploymentPostprocessing = 0;')
common = '''CLANG_ENABLE_MODULES = YES; SWIFT_VERSION = 5.0; IPHONEOS_DEPLOYMENT_TARGET = 15.0; SDKROOT = iphoneos; TARGETED_DEVICE_FAMILY = "1,2"; ENABLE_USER_SCRIPT_SANDBOXING = YES;'''
configs = {}
for name in ['Debug', 'Release']:
    project = add('project:'+name, f'isa = XCBuildConfiguration; name = {name}; buildSettings = {{ {common} }};')
    settings = '''PRODUCT_NAME = "$(TARGET_NAME)"; PRODUCT_BUNDLE_IDENTIFIER = vn.local.suppermario; INFOPLIST_FILE = App/Info.plist;
    GENERATE_INFOPLIST_FILE = NO; CODE_SIGN_STYLE = Automatic; DEVELOPMENT_TEAM = "";
    CURRENT_PROJECT_VERSION = 1; MARKETING_VERSION = 1.0; ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
    SUPPORTED_PLATFORMS = "iphoneos iphonesimulator"; SUPPORTS_MACCATALYST = NO;
    LD_RUNPATH_SEARCH_PATHS = "$(inherited) @executable_path/Frameworks";
    SWIFT_EMIT_LOC_STRINGS = YES; DEBUG_INFORMATION_FORMAT = "dwarf-with-dsym";
    '''
    settings += 'SWIFT_OPTIMIZATION_LEVEL = "-Onone"; SWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG; ENABLE_TESTABILITY = YES;' if name == 'Debug' else 'SWIFT_OPTIMIZATION_LEVEL = "-O";'
    target = add('target:'+name, f'isa = XCBuildConfiguration; name = {name}; buildSettings = {{ {settings} }};')
    configs[name] = (project, target)
for i, kind in enumerate(['project', 'target']):
    add(kind+'Configs', f'isa = XCConfigurationList; buildConfigurations = ({configs["Debug"][i]}, {configs["Release"][i]}); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release;')
add('target', f'''isa = PBXNativeTarget; name = SupperMario; productName = SupperMario; productType = "com.apple.product-type.application";
    productReference = {product}; buildConfigurationList = {ident("targetConfigs")}; buildPhases = ({sources}, {frameworks}, {resources}); buildRules = (); dependencies = ();''')
add('project', f'''isa = PBXProject; attributes = {{ LastUpgradeCheck = 1640; }}; buildConfigurationList = {ident("projectConfigs")}; compatibilityVersion = "Xcode 14.0";
    developmentRegion = vi; hasScannedForEncodings = 0; knownRegions = (vi, en, Base); mainGroup = {ident("mainGroup")}; productRefGroup = {products}; projectDirPath = ""; projectRoot = ""; targets = ({ident("target")}, {ident("uitestTarget")});''')
path = ROOT / 'ios/SupperMario.xcodeproj'
path.mkdir(parents=True, exist_ok=True)
(path/'project.pbxproj').write_text('// !$*UTF8*$!\n{ archiveVersion = 1; classes = {}; objectVersion = 56; objects = {\n'+'\n'.join(objects)+f'\n }}; rootObject = {ident("project")}; }}\n')
scheme = path/'xcshareddata/xcschemes/SupperMario.xcscheme'
scheme.parent.mkdir(parents=True, exist_ok=True)
scheme.write_text(f'''<?xml version="1.0" encoding="UTF-8"?>
<Scheme LastUpgradeVersion="1640" version="1.3">
<BuildAction parallelizeBuildables="YES" buildImplicitDependencies="YES"><BuildActionEntries><BuildActionEntry buildForTesting="YES" buildForRunning="YES" buildForProfiling="YES" buildForArchiving="YES" buildForAnalyzing="YES"><BuildableReference BuildableIdentifier="primary" BlueprintIdentifier="{ident('target')}" BuildableName="SupperMario.app" BlueprintName="SupperMario" ReferencedContainer="container:SupperMario.xcodeproj"/></BuildActionEntry></BuildActionEntries></BuildAction>
<TestAction buildConfiguration="Debug" selectedDebuggerIdentifier="Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier="Xcode.IDEFoundation.Launcher.LLDB" shouldUseLaunchSchemeArgsEnv="YES"><Testables><TestableReference skipped="NO"><BuildableReference BuildableIdentifier="primary" BlueprintIdentifier="{ident('uitestTarget')}" BuildableName="NativeGameUITests.xctest" BlueprintName="NativeGameUITests" ReferencedContainer="container:SupperMario.xcodeproj"/></TestableReference></Testables></TestAction>
<LaunchAction buildConfiguration="Debug" selectedDebuggerIdentifier="Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier="Xcode.IDEFoundation.Launcher.LLDB" launchStyle="0" useCustomWorkingDirectory="NO" ignoresPersistentStateOnLaunch="NO" debugDocumentVersioning="YES" debugServiceExtension="internal" allowLocationSimulation="YES"><BuildableProductRunnable runnableDebuggingMode="0"><BuildableReference BuildableIdentifier="primary" BlueprintIdentifier="{ident('target')}" BuildableName="SupperMario.app" BlueprintName="SupperMario" ReferencedContainer="container:SupperMario.xcodeproj"/></BuildableProductRunnable></LaunchAction>
<ProfileAction buildConfiguration="Release" shouldUseLaunchSchemeArgsEnv="YES" savedToolIdentifier="" useCustomWorkingDirectory="NO" debugDocumentVersioning="YES"/>
<AnalyzeAction buildConfiguration="Debug"/><ArchiveAction buildConfiguration="Release" revealArchiveInOrganizer="YES"/>
</Scheme>''')
print('Generated SupperMario.xcodeproj')
