/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\workspace-eclipse\\APU_tele_movi_new\\src\\net\\cp\\ac\\core\\SyncEngineInterface.aidl
 */
package net.cp.ac.core;
/**
 * This interface defines the most important methods of the SyncEngineService.
 * It is also used to define the RPC interface between the service and any attached UIs
 */
public interface SyncEngineInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements net.cp.ac.core.SyncEngineInterface
{
private static final java.lang.String DESCRIPTOR = "net.cp.ac.core.SyncEngineInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an net.cp.ac.core.SyncEngineInterface interface,
 * generating a proxy if needed.
 */
public static net.cp.ac.core.SyncEngineInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof net.cp.ac.core.SyncEngineInterface))) {
return ((net.cp.ac.core.SyncEngineInterface)iin);
}
return new net.cp.ac.core.SyncEngineInterface.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_startSync:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
boolean _result = this.startSync(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getSyncState:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getSyncState();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_abortSync:
{
data.enforceInterface(DESCRIPTOR);
this.abortSync();
reply.writeNoException();
return true;
}
case TRANSACTION_suspendSync:
{
data.enforceInterface(DESCRIPTOR);
this.suspendSync();
reply.writeNoException();
return true;
}
case TRANSACTION_resumeSync:
{
data.enforceInterface(DESCRIPTOR);
this.resumeSync();
reply.writeNoException();
return true;
}
case TRANSACTION_getLastProgress:
{
data.enforceInterface(DESCRIPTOR);
net.cp.ac.core.ParcelableSyncProgress _result = this.getLastProgress();
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_registerCallback:
{
data.enforceInterface(DESCRIPTOR);
net.cp.ac.ui.UICallbackInterface _arg0;
_arg0 = net.cp.ac.ui.UICallbackInterface.Stub.asInterface(data.readStrongBinder());
this.registerCallback(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setServerAlertConsumer:
{
data.enforceInterface(DESCRIPTOR);
net.cp.ac.ui.UICallbackInterface _arg0;
_arg0 = net.cp.ac.ui.UICallbackInterface.Stub.asInterface(data.readStrongBinder());
this.setServerAlertConsumer(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setCISConsumer:
{
data.enforceInterface(DESCRIPTOR);
net.cp.ac.ui.UICallbackInterface _arg0;
_arg0 = net.cp.ac.ui.UICallbackInterface.Stub.asInterface(data.readStrongBinder());
this.setCISConsumer(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setPISConsumer:
{
data.enforceInterface(DESCRIPTOR);
net.cp.ac.ui.UICallbackInterface _arg0;
_arg0 = net.cp.ac.ui.UICallbackInterface.Stub.asInterface(data.readStrongBinder());
this.setPISConsumer(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getNumberChangedContacts:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
int _result = this.getNumberChangedContacts(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getBatteryPercent:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getBatteryPercent();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements net.cp.ac.core.SyncEngineInterface
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
/**
     * Initiate sync sessions for the given types
     * @param syncMediaTypes The types of items to sync
     * @return true if the sync was successfully started, otherwise false.
     */
@Override public boolean startSync(int syncMediaTypes) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(syncMediaTypes);
mRemote.transact(Stub.TRANSACTION_startSync, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
     * This method is used to identify what state the sync is in.
     * @return one of the following in order of decreasing precedance:
     * 
     * StatusCodes.SYNC_ABORTING
     * StatusCodes.SYNC_SUSPENDING
     * StatusCodes.SYNC_RESUMING
     * StatusCodes.SYNC_SUSPENDED
     * StatusCodes.SYNC_IN_PROGRESS
     * StatusCodes.NONE
     * 
     */
@Override public int getSyncState() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getSyncState, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/** call this to abort a currently running sync */
@Override public void abortSync() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_abortSync, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Called to suspend the sync session in progress.
     * The session can be resumed for a certain period of time before it is aborted.
     */
@Override public void suspendSync() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_suspendSync, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Called to resume the suspended sync session.
     */
@Override public void resumeSync() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_resumeSync, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/** get the lastest progress/status of the current sync. null if we are not currently syncing */
@Override public net.cp.ac.core.ParcelableSyncProgress getLastProgress() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
net.cp.ac.core.ParcelableSyncProgress _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getLastProgress, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = net.cp.ac.core.ParcelableSyncProgress.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/** pass in an interface to a remote UI. This will be used to report sync status and progress */
@Override public void registerCallback(net.cp.ac.ui.UICallbackInterface uiInterface) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((uiInterface!=null))?(uiInterface.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_registerCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * pass in an interface to a remote UI. This will be the UI that responds to server alerts.
     * Only one UI can get the alerts, so subsequent calls to this method replace the old consumer.
     * Calling this with null removes the last set consumer
     */
@Override public void setServerAlertConsumer(net.cp.ac.ui.UICallbackInterface uiInterface) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((uiInterface!=null))?(uiInterface.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_setServerAlertConsumer, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * pass in an interface to a remote UI. This will be the UI that responds to CIS event.
     * Only one UI can get the event, so subsequent calls to this method replace the old consumer.
     * Calling this with null removes the last set consumer
     */
@Override public void setCISConsumer(net.cp.ac.ui.UICallbackInterface uiInterface) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((uiInterface!=null))?(uiInterface.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_setCISConsumer, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * pass in an interface to a remote UI. This will be the UI that responds to periodic sync event.
     * Only one UI can get the event, so subsequent calls to this method replace the old consumer.
     * Calling this with null removes the last set consumer
     */
@Override public void setPISConsumer(net.cp.ac.ui.UICallbackInterface uiInterface) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((uiInterface!=null))?(uiInterface.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_setPISConsumer, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * This calculates the number of contacts that have changed since the last sync,
     * by running through the contacts and calculating the sync state.
     * This is an expensive operation, so this method should be called sparingly.

     * @param saveChangelogs if true, the changelogs calculated here will be used in the subsequent sync. 
     * @return the number of contacts that have changed since the last sync,
     * or -1 if the operation could not be completed.
     */
@Override public int getNumberChangedContacts(boolean saveChangelogs) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((saveChangelogs)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_getNumberChangedContacts, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
     * @return The battery level in percent, or 100 if unknown
     */
@Override public int getBatteryPercent() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getBatteryPercent, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_startSync = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_getSyncState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_abortSync = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_suspendSync = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_resumeSync = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_getLastProgress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_registerCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_setServerAlertConsumer = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_setCISConsumer = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_setPISConsumer = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_getNumberChangedContacts = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_getBatteryPercent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
}
/**
     * Initiate sync sessions for the given types
     * @param syncMediaTypes The types of items to sync
     * @return true if the sync was successfully started, otherwise false.
     */
public boolean startSync(int syncMediaTypes) throws android.os.RemoteException;
/**
     * This method is used to identify what state the sync is in.
     * @return one of the following in order of decreasing precedance:
     * 
     * StatusCodes.SYNC_ABORTING
     * StatusCodes.SYNC_SUSPENDING
     * StatusCodes.SYNC_RESUMING
     * StatusCodes.SYNC_SUSPENDED
     * StatusCodes.SYNC_IN_PROGRESS
     * StatusCodes.NONE
     * 
     */
public int getSyncState() throws android.os.RemoteException;
/** call this to abort a currently running sync */
public void abortSync() throws android.os.RemoteException;
/**
     * Called to suspend the sync session in progress.
     * The session can be resumed for a certain period of time before it is aborted.
     */
public void suspendSync() throws android.os.RemoteException;
/**
     * Called to resume the suspended sync session.
     */
public void resumeSync() throws android.os.RemoteException;
/** get the lastest progress/status of the current sync. null if we are not currently syncing */
public net.cp.ac.core.ParcelableSyncProgress getLastProgress() throws android.os.RemoteException;
/** pass in an interface to a remote UI. This will be used to report sync status and progress */
public void registerCallback(net.cp.ac.ui.UICallbackInterface uiInterface) throws android.os.RemoteException;
/**
     * pass in an interface to a remote UI. This will be the UI that responds to server alerts.
     * Only one UI can get the alerts, so subsequent calls to this method replace the old consumer.
     * Calling this with null removes the last set consumer
     */
public void setServerAlertConsumer(net.cp.ac.ui.UICallbackInterface uiInterface) throws android.os.RemoteException;
/**
     * pass in an interface to a remote UI. This will be the UI that responds to CIS event.
     * Only one UI can get the event, so subsequent calls to this method replace the old consumer.
     * Calling this with null removes the last set consumer
     */
public void setCISConsumer(net.cp.ac.ui.UICallbackInterface uiInterface) throws android.os.RemoteException;
/**
     * pass in an interface to a remote UI. This will be the UI that responds to periodic sync event.
     * Only one UI can get the event, so subsequent calls to this method replace the old consumer.
     * Calling this with null removes the last set consumer
     */
public void setPISConsumer(net.cp.ac.ui.UICallbackInterface uiInterface) throws android.os.RemoteException;
/**
     * This calculates the number of contacts that have changed since the last sync,
     * by running through the contacts and calculating the sync state.
     * This is an expensive operation, so this method should be called sparingly.

     * @param saveChangelogs if true, the changelogs calculated here will be used in the subsequent sync. 
     * @return the number of contacts that have changed since the last sync,
     * or -1 if the operation could not be completed.
     */
public int getNumberChangedContacts(boolean saveChangelogs) throws android.os.RemoteException;
/**
     * @return The battery level in percent, or 100 if unknown
     */
public int getBatteryPercent() throws android.os.RemoteException;
}
