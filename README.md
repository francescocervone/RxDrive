[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RxDrive-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/3479)
[![](https://jitpack.io/v/francescocervone/rxdrive.svg)](https://jitpack.io/#francescocervone/rxdrive)


# RxDrive
RxJava wrapper for Google Drive Android API

## Usage
### Connecting
```
public class MyActivity extends AppCompatActivity {
    private RxDrive mRxDrive;
    private Subscription mSubscription;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	...
    	mRxDrive = new RxDrive(this);
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
        mSubscriptions.unsubscribe();
    }
    
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        mRxDrive.onActivityResult(requestCode, resultCode, data);
    }
}
```

### Creating a file
```
mRxDrive.createFile(mRxDrive.getAppFolder(), uriOrFile, optionalName, optionalMimeType)
    .subscribe(new Action1<DriveId>() {
        @Override
        public void call(DriveId driveId) { ... }
    }, new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) { ... }
    });
```

### Listing children of a folder
```
mRxDrive.listChildren(mRxDrive.getAppFolder())
    .subscribe(new Action1<List<DriveId>>() {
        @Override
        public void call(List<DriveId> driveIds) { ... }
    }, new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) { ... }
    });
```
### Querying for children of a folder
```
Query query = new Query.Builder()
	.addFilter(Filters.eq(SearchableField.TITLE, "HelloWorld.java"))
	.build()
mRxDrive.queryChildren(mRxDrive.getAppFolder(), query)
    .subscribe(new Action1<List<DriveId>>() {
        @Override
        public void call(List<DriveId> driveIds) { ... }
    }, new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) { ... }
    });
```

### Getting metadata
```
mRxDrive.getMetadata(someDriveId)
	.subscribe(new Action1<Metadata>() {
	    @Override
            public void call(Metadata metadata) { ... }
	}, new Action1<Throwable>() {
	    @Override
            public void call(Throwable throwable) { ... }
	}
```

### Opening a file
```
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
    .subscribe(new Action1<InputStream>() {
    	@Override
    	public void call(InputStream inputStream) { ... }
    }, new Action1<Throwable>() {
    	@Override
    	public void call(Throwable throwable) { ... }
    });
```
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
  compile 'com.github.francescocervone:rxdrive:0.1'
}
```
