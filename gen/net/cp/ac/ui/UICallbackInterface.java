/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\workspace-eclipse\\APU_tele_movi_new\\src\\net\\cp\\ac\\ui\\UICallbackInterface.aidl
 */
package net.cp.ac.ui;
/**
 * Defines the interface the service will use to send information to the UI.
 * It is almost identical to UIInterface, except it contains RPC parcelling information.
 *
 * @see net.cp.engine.UIInterface
 */
public interface UICallbackInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements net.cp.ac.ui.UICallbackInterface
{
private static final java.lang.String DESCRIPTOR = "net.cp.ac.ui.UICallbackInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an net.cp.ac.ui.UICallbackInterface interface,
 * generating a proxy if needed.
 */
public static net.cp.ac.ui.UICallbackInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof net.cp.ac.ui.UICallbackInterface))) {
return ((net.cp.ac.ui.UICallbackInterface)iin);
}
return new net.cp.ac.ui.UICallbackInterface.Stub.Proxy(obj);
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
case TRANSACTION_updateSyncProgress:
{
data.enforceInterface(DESCRIPTOR);
net.cp.ac.core.ParcelableSyncProgress _arg0;
if ((0!=data.readInt())) {
_arg0 = net.cp.ac.core.ParcelableSyncProgress.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.updateSyncProgress(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_onGetChangesEnd:
{
data.enforceInterface(DESCRIPTOR);
this.onGetChangesEnd();
reply.writeNoException();
return true;
}
case TRANSACTION_onSyncError:
{
data.enforceInterface(DESCRIPTOR);
this.onSyncError();
reply.writeNoException();
return true;
}
case TRANSACTION_onSyncEnd:
{
data.enforceInterface(DESCRIPTOR);
this.onSyncEnd();
reply.writeNoException();
return true;
}
case TRANSACTION_serverAlertReceived:
{
data.enforceInterface(DESCRIPTOR);
byte[] _arg0;
_arg0 = data.createByteArray();
this.serverAlertReceived(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_onItemsChanged:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
this.onItemsChanged(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_onAlertSlowSync:
{
data.enforceInterface(DESCRIPTOR);
this.onAlertSlowSync();
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements net.cp.ac.ui.UICallbackInterface
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
     * Updates the sync session progress with the specified details.
     * 
     * @param progress that current status of the sync
     */
@Override public void updateSyncProgress(net.cp.ac.core.ParcelableSyncProgress progress) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((progress!=null)) {
_data.writeInt(1);
progress.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_updateSyncProgress, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/** Called when the engine has finished calculating the changes. */
@Override public void onGetChangesEnd() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onGetChangesEnd, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/** Called when an error occurs during the sync session. */
@Override public void onSyncError() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onSyncError, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/** Called when the sync has ended, no matter how. */
@Override public void onSyncEnd() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onSyncEnd, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/** Only called if this UI is registered to receive Server Alerts.
     *  Called when a verified Server Alert has been received.
     *  @param data The server alert payload, from which the full alert can be re-created
     */
@Override public void serverAlertReceived(byte[] data) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeByteArray(data);
mRemote.transact(Stub.TRANSACTION_serverAlertReceived, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/** Called when syncable items have changed, and it is time to sync.
     *  Only called if this UI is registered to receive CIS notifications.
     *  @param mediaType the type of items that have changed.
     *  Currently only EngineSettings.MEDIA_TYPE_CONTACTS supported
     *  @param numberOfChanges the number of changes detected,
     *  or -1 if this information could not be determined.
     *  Note that if settings.contactMinSyncLimit is set to 0, numberOfChanges will always be -1.
     */
@Override public void onItemsChanged(int mediaType, int numberOfChanges) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(mediaType);
_data.writeInt(numberOfChanges);
mRemote.transact(Stub.TRANSACTION_onItemsChanged, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onAlertSlowSync() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onAlertSlowSync, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_updateSyncProgress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onGetChangesEnd = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_onSyncError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_onSyncEnd = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_serverAlertReceived = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_onItemsChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_onAlertSlowSync = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
}
/** 
     * Updates the sync session progress with the specified details.
     * 
     * @param progress that current status of the sync
     */
public void updateSyncProgress(net.cp.ac.core.ParcelableSyncProgress progress) throws android.os.RemoteException;
/** Called when the engine has finished calculating the changes. */
public void onGetChangesEnd() throws android.os.RemoteException;
/** Called when an error occurs during the sync session. */
public void onSyncError() throws android.os.RemoteException;
/** Called when the sync has ended, no matter how. */
public void onSyncEnd() throws android.os.RemoteException;
/** Only called if this UI is registered to receive Server Alerts.
     *  Called when a verified Server Alert has been received.
     *  @param data The server alert payload, from which the full alert can be re-created
     */
public void serverAlertReceived(byte[] data) throws android.os.RemoteException;
/** Called when syncable items have changed, and it is time to sync.
     *  Only called if this UI is registered to receive CIS notifications.
     *  @param mediaType the type of items that have changed.
     *  Currently only EngineSettings.MEDIA_TYPE_CONTACTS supported
     *  @param numberOfChanges the number of changes detected,
     *  or -1 if this information could not be determined.
     *  Note that if settings.contactMinSyncLimit is set to 0, numberOfChanges will always be -1.
     */
public void onItemsChanged(int mediaType, int numberOfChanges) throws android.os.RemoteException;
public void onAlertSlowSync() throws android.os.RemoteException;
}
