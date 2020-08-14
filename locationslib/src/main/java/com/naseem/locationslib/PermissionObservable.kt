package com.naseem.locationslib

import java.util.*

object PermissionObservable : Observable() {
   fun notifyResult(result: LocationRequester.PermissionResult) {
       setChanged()
       notifyObservers(result)
   }
}