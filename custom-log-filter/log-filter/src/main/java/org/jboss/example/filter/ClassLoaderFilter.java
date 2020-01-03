/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2019 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.example.filter;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

import org.jboss.logging.MDC;
import org.jboss.modules.ModuleClassLoader;

/**
 * A {@linkplain Filter log filter} which attempts to get the name from a modular class loader and if the name of the
 * module matches the {@linkplain #getPattern() pattern} then the message will be logged.
 * <p>
 * Optionally you can add {@linkplain #setLoggerNames(String) logger names} in a comma delimited string to only allow
 * specific logger names to be logged.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("unused")
public class ClassLoaderFilter implements Filter {

    private volatile Pattern pattern;
    private final Set<String> loggerNames = Collections.synchronizedSet(new HashSet<>());

    @Override
    public boolean isLoggable(final LogRecord record) {
        if (!loggerNames.isEmpty() && !loggerNames.contains(record.getLoggerName())) {
            return false;
        }
        final ClassLoader cl = getClassLoader();
        String value;
        if (cl instanceof ModuleClassLoader) {
            value = ((ModuleClassLoader) cl).getName();
        } else {
            value = cl.toString();
        }
        if (pattern == null || pattern.matcher(value).matches()) {
            MDC.put("moduleName", value);
            return true;
        }
        MDC.remove("moduleName");
        return false;
    }

    /**
     * Returns the collection of logger names or an empty collection.
     *
     * @return the logger names or an empty collection
     */
    public Set<String> getLoggerNames() {
        synchronized (loggerNames) {
            return new HashSet<>(loggerNames);
        }
    }

    /**
     * Sets which logger names are allowed to pass the filter. This is a comma delimited string. If {@code null} an
     * empty list is used and all logger will be checked.
     *
     * @param loggerNames a comma delimited set of logger names or {@code null} to allow for all logger names
     */
    public void setLoggerNames(final String loggerNames) {
        if (loggerNames == null) {
            this.loggerNames.clear();
        } else {
            Collections.addAll(this.loggerNames, loggerNames.split(","));
        }
    }

    /**
     * Returns the pattern used to check the against the module name, assuming the class loader is a
     * {@link ModuleClassLoader}. Otherwise the {@link ClassLoader#toString()} is checked.
     *
     * @return the pattern used for matching
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * Sets the pattern used for checking the class loader.
     *
     * @param pattern the pattern
     */
    public void setPattern(final String pattern) {
        this.pattern = Pattern.compile(Objects.requireNonNull(pattern, "The pattern cannot be null."));
    }

    private static ClassLoader getClassLoader() {
        if (System.getSecurityManager() == null) {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            return cl == null ? ClassLoaderFilter.class.getClassLoader() : cl;
        }
        return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            return cl == null ? ClassLoaderFilter.class.getClassLoader() : cl;
        });
    }
}
