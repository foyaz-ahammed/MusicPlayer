package com.kr.musicplayer.misc.manager;

import android.app.Service;
import java.util.ArrayList;

/**
 * Service 관리를 위한 class
 */
public class ServiceManager {

  private static ArrayList<Service> mServiceList = new ArrayList<>();

  public static void AddService(Service service) {
    mServiceList.add(service);
  }

  public static void RemoveService(Service service) {
    mServiceList.remove(service);
  }

  public static void StopAll() {
    for (Service service : mServiceList) {
      if (service != null) {
        service.stopSelf();
      }
    }
  }
}
