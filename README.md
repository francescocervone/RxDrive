# RxDrive
RxJava wrapper for Google Drive Android API

## Gradle
Add in your root `build.gradle` at the end of repositories:
```
allprojects {
	repositories {
		...
		maven { url "https://jitpack.io" }
	}
}
```

Add in your app `build.gradle` the dependency:
```
dependencies {
  ...
  compile 'com.github.francescocervone:rxdrive:master-SNAPSHOT'
}
```
