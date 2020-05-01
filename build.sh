#!/bin/sh

sdk=$ANDROID_HOME

outputDirForGeneratedSourceFiles='generated_java_sources'
outputDirForBytecode='java_virtual_machine_bytecode'
outputDexFilepath='classes.dex'

filepathOfAPK="app.apk"
filepathOfUnalignedAPK="${filepathOfAPK}.unaligned"


# Use the latest build tools version...
buildTools=$sdk/build-tools/$(/bin/ls $sdk/build-tools | sort -n | tail -1)

# ...and the latest platform version.
platform=$sdk/platforms/$(/bin/ls $sdk/platforms | grep android-25)
test -z "$platform" && \
	echo "platform 25 is needed to build against: try $SDK_HOME/bin/sdkmanager 'platforms;android-25'" && \
	exit 1

androidLib=$platform/android.jar

_aapt=$buildTools/aapt
_dx=$buildTools/dx

manifestFilepath=$PWD/AndroidManifest.xml
resourcesFilepath=$PWD/xml
javaSourcesFilepath=$PWD/java

main() {
	makeOutputDirs && \
	generateJavaFileForAndroidResources && \
	compileJavaSourceFilesToJavaVirtualMachineBytecode && \
	translateJavaVirtualMachineMBytecodeToAndroidRuntimeBytecode && \
	createUnalignedAndroidApplicationPackage && \
	addAndroidRuntimeBytecodeToAndroidApplicationPackage && \
	signAndroidApplicationPackageWithDebugKey && \
	alignUncompressedDataInZipFileToFourByteBoundariesForFasterMemoryMappingAtRuntime  && \
	cleanup
}

makeOutputDirs() {
	mkdir -p "$outputDirForBytecode" "$outputDirForGeneratedSourceFiles"
}

generateJavaFileForAndroidResources() {
	# aapt package
	#
	#   Package the android resources.  It will read assets and resources that are
	#   supplied with the -M -A -S or raw-files-dir arguments.  The -J -P -F and -R
	#   options control which files are output.
	#
	#	-f  force overwrite of existing files
	#   -m  make package directories under location specified by -J
	#   -J  specify where to output R.java resource constant definitions
	#	-M  specify full path to AndroidManifest.xml to include in zip
	#	-S  directory in which to find resources.  Multiple directories will be scanned
	#       and the first match found (left to right) will take precedence.
	#   -I  add an existing package to base include set
	$_aapt package -f -m -J "$outputDirForGeneratedSourceFiles" -M "$manifestFilepath" -S "$resourcesFilepath" -I "$androidLib"
}

compileJavaSourceFilesToJavaVirtualMachineBytecode() {
	echo "TODO: Why aren't .aar files (or the extracted classes.jar)" && \
	echo "detected by javac even when specified in the -classpath flag?" && \
	javac \
		-classpath "$androidLib:/tmp/c/classes.jar" \
		-sourcepath "$javaSourcesFilepath:$outputDirForGeneratedSourceFiles" \
		-d "$outputDirForBytecode" \
		-target 1.7 \
		-source 1.7 \
		$(find $javaSourcesFilepath -name '*.java') \
		$(find $outputDirForGeneratedSourceFiles -name '*.java')
}

translateJavaVirtualMachineMBytecodeToAndroidRuntimeBytecode() {
	$_dx --dex --output="$outputDexFilepath" "$outputDirForBytecode"
}

createUnalignedAndroidApplicationPackage() {
	$_aapt package -f -M "$manifestFilepath" -S "$resourcesFilepath" -I "$androidLib" -F "$filepathOfUnalignedAPK"
}

addAndroidRuntimeBytecodeToAndroidApplicationPackage() {
	( $_aapt add "$filepathOfUnalignedAPK" "$outputDexFilepath" ) 1>&2
}

signAndroidApplicationPackageWithDebugKey() {
	( jarsigner -keystore "$HOME/.android/debug.keystore" -storepass 'android' "$filepathOfUnalignedAPK" androiddebugkey ) 1>&2
}

alignUncompressedDataInZipFileToFourByteBoundariesForFasterMemoryMappingAtRuntime() {
	$buildTools/zipalign -f 4 "$filepathOfUnalignedAPK" "$filepathOfAPK"
}

cleanup() {
	rm -rf "$outputDirForBytecode" "$outputDirForGeneratedSourceFiles" "$filepathOfUnalignedAPK" "$outputDexFilepath"
}

main
