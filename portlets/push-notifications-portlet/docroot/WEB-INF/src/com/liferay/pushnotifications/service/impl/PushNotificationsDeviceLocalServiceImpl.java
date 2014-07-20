/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.pushnotifications.service.impl;

import com.liferay.portal.kernel.bean.BeanReference;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.pushnotifications.model.PushNotificationsDevice;
import com.liferay.pushnotifications.sender.PushNotificationsSender;
import com.liferay.pushnotifications.service.base.PushNotificationsDeviceLocalServiceBaseImpl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Silvio Santos
 * @author Bruno Farache
 */
public class PushNotificationsDeviceLocalServiceImpl
	extends PushNotificationsDeviceLocalServiceBaseImpl {

	@Override
	public PushNotificationsDevice addPushNotificationsDevice(
		long userId, String platform, String token) {

		long pushNotificationsDeviceId = counterLocalService.increment();

		PushNotificationsDevice pushNotificationsDevice =
			pushNotificationsDevicePersistence.create(
				pushNotificationsDeviceId);

		pushNotificationsDevice.setUserId(userId);
		pushNotificationsDevice.setCreateDate(new Date());
		pushNotificationsDevice.setPlatform(platform);
		pushNotificationsDevice.setToken(token);

		pushNotificationsDevicePersistence.update(pushNotificationsDevice);

		return pushNotificationsDevice;
	}

	@Override
	public PushNotificationsDevice deletePushNotificationsDevice(String token)
		throws PortalException {

		PushNotificationsDevice pushNotificationsDevice =
			pushNotificationsDevicePersistence.findByToken(token);

		pushNotificationsDevicePersistence.remove(pushNotificationsDevice);

		return pushNotificationsDevice;
	}

	@Override
	public void sendPushNotification(JSONObject jsonObject, int start, int end)
		throws PortalException {

		sendPushNotification(_ALL_USERS_USER_ID, jsonObject, start, end);
	}

	@Override
	public void sendPushNotification(
			long userId, JSONObject jsonObject, int start, int end)
		throws PortalException {

		for (Map.Entry<String, PushNotificationsSender> entry :
				_pushNotificationsSenders.entrySet()) {

			List<String> tokens = new ArrayList<String>();

			List<PushNotificationsDevice> pushNotificationsDevices =
				getPushNotificationsDevices(userId, entry.getKey(), start, end);

			for (PushNotificationsDevice pushNotificationsDevice :
					pushNotificationsDevices) {

				tokens.add(pushNotificationsDevice.getToken());
			}

			if (tokens.isEmpty()) {
				continue;
			}

			PushNotificationsSender pushNotificationsSender = entry.getValue();

			try {
				pushNotificationsSender.send(tokens, jsonObject);
			}
			catch (PortalException pe) {
				throw pe;
			}
			catch (Exception e) {
				throw new PortalException(e);
			}
		}
	}

	protected List<PushNotificationsDevice> getPushNotificationsDevices(
		long userId, String platform, int start, int end) {

		if (userId == _ALL_USERS_USER_ID) {
			return pushNotificationsDevicePersistence.findByPlatform(
				platform, start, end);
		}

		return pushNotificationsDevicePersistence.findByU_P(
			userId, platform, start, end);
	}

	private static final int _ALL_USERS_USER_ID = -1;

	@BeanReference(name = "pushNotificationsSenders")
	private Map<String, PushNotificationsSender> _pushNotificationsSenders;

}