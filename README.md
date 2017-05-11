[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RxDrive-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/3479)
[ ![Download](https://api.bintray.com/packages/francescocervone/maven/rxdrive/images/download.svg) ](https://bintray.com/francescocervone/maven/rxdrive/_latestVersion)
[![Build Status](https://travis-ci.org/francescocervone/RxDrive.svg?branch=develop)](https://travis-ci.org/francescocervone/RxDrive)


# RxDrive
RxJava wrapper for Google Drive Android API

## Why
Using Google Drive API for Android can be a little frustrating because sometimes you must define nested callbacks. If you want to avoid them, you should create an `AsyncTask` for each action and call synchronous methods (`await()`). Anyway, you have to write some ugly and confusing code.

The purpose of RxDrive is to use Google Drive APIs with the elegance and the advantages of RxJava.

No more nested callbacks, no more `AsyncTasks`.

## Before you start
Before you start using this library, you need to follow the instructions to create a new project with Google Drive API access (https://developers.google.com/drive/android/get-started).

## Features
Since this project is still a work in progress, additional features will come in next releases. Currently, RxDrive allows you to:
* Authenticate users
* Notify your app about changes of the connection state
* Create files
* Open files
* Update files
* Get metadata of Drive resources
* List and query resources
* Trash, untrash and delete Drive resources
* Sync Drive

## Examples
### Connecting
```java
public class MyActivity extends AppCompatActivity {
    private RxDrive mRxDrive;
    private Subscription mSubscription;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	...
    	mRxDrive = new RxDrive(new GoogleApiClient.Builder(this)
    		.addApi(Drive.API)
    		.addScope(Drive.SCOPE_FILE) //If you want to access to user files
    		.addScope(Drive.SCOPE_APPFOLDER) //If you want to access to the app folder
    	);
    }
    	
    @Override
    protected void onStart() {
        ...
        mSubscription = mRxDrive.connectionObservable()
                .subscribe(new Action1<ConnectionState>() {
                    @Override
                    public void call(ConnectionState connectionState) {
                        switch (connectionState.getState()) {
                            case CONNECTED:
                                doSomething(connectionState.getBundle());
                                break;
                            case SUSPENDED:
                                doSomethingElse(connectionState.getCause());
                                break;
                            case FAILED:
                                mRxDrive.resolveConnection(MyActivity.this, connectionState.getConnectionResult());
                                break;
                            case UNABLE_TO_RESOLVE:
                                showMessageToUser(connectionState.getConnectionResult());
                                break;
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) { ... }
                });
        mRxDrive.connect();
    }
    
    @Override
    protected void onStop() {
        ...
        mRxDrive.disconnect();
        mSubscription.unsubscribe();
    }
    
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        mRxDrive.onActivityResult(requestCode, resultCode, data);
    }
}
```

### Creating a file
```java
mRxDrive.createFile(mRxDrive.getAppFolder(), uriOrFile, optionalName, optionalMimeType)
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(new Action1<DriveId>() {
        @Override
        public void call(DriveId driveId) { ... }
    }, new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) { ... }
    });
```

### Listing children of a folder
```java
mRxDrive.listChildren(mRxDrive.getAppFolder())
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(new Action1<List<DriveId>>() {
        @Override
        public void call(List<DriveId> driveIds) { ... }
    }, new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) { ... }
    });
```
### Querying for children of a folder
```java
Query query = new Query.Builder()
	.addFilter(Filters.eq(SearchableField.TITLE, "HelloWorld.java"))
	.build()
mRxDrive.queryChildren(mRxDrive.getAppFolder(), query)
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(new Action1<List<DriveId>>() {
        @Override
        public void call(List<DriveId> driveIds) { ... }
    }, new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) { ... }
    });
```

### Getting metadata
```java
mRxDrive.getMetadata(someDriveId)
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(new Action1<Metadata>() {
	@Override
        public void call(Metadata metadata) { ... }
	}, new Action1<Throwable>() {
	@Override
        public void call(Throwable throwable) { ... }
	}
```

### Opening a file
```java
mRxDrive.open(mDriveId, new Subscriber<Progress>() {
        @Override
        public void onCompleted() { ... }
    
        @Override
        public void onError(Throwable e) { ... }
    
        @Override
        public void onNext(Progress progress) {
            mTextView.setText(progress.getPercentage() + "%");
        }
    })
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(new Action1<InputStream>() {
    	@Override
    	public void call(InputStream inputStream) { ... }
    }, new Action1<Throwable>() {
    	@Override
    	public void call(Throwable throwable) { ... }
    });
```
## Gradle
Add in your root `build.gradle`:
```gradle
repositories {
	jcenter()
}
```

Add in your app `build.gradle` the dependency:
```gradle
dependencies {
  ...
  compile 'com.francescocervone:rxdrive:0.2'
}
```
