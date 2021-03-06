/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.overlord.commons.auth;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * i18n messages.
 *
 * @author eric.wittmann@redhat.com
 */
public class Messages {

    private static final String BUNDLE_NAME = "org.overlord.commons.auth.messages"; //$NON-NLS-1$
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    /**
     * Constructor.
     */
    private Messages() {
    }

    /**
     * Gets a string translation.
     * @param key
     * @return the translated string
     */
    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    /**
     * Look up a message in the i18n resource message bundle by key, then format the
     * message with the given params and return the result.
     * @param key
     * @param params
     * @return the translated and formatted string
     */
    public static String format(String key, Object ... params) {
        String msg = getString(key);
        try {
            return MessageFormat.format(msg, params);
        } catch (Exception e) {
            return '!' + key + '!';
        }
    }

}
