/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Datapipe, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.datapipe.jenkins.vault.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static hudson.Util.fixEmptyAndTrim;

/**
 * @author Peter Tierno {@literal <}ptierno{@literal @}datapipe.com{@literal >}
 */
public class VaultSecretValue
    extends AbstractDescribableImpl<VaultSecretValue> {

    private String envVar;
    private final String vaultKey;
    private String vaultSecret;

    @Deprecated
    public VaultSecretValue(String envVar, @NonNull String vaultKey) {
        this.envVar = fixEmptyAndTrim(envVar);
        this.vaultKey = fixEmptyAndTrim(vaultKey);
    }

    @DataBoundConstructor
    public VaultSecretValue(@NonNull String vaultKey) {
        this.vaultKey = fixEmptyAndTrim(vaultKey);
    }

    @DataBoundSetter
    public void setEnvVar(String envVar) {
        this.envVar = envVar;
    }

    @DataBoundSetter
    public void setVaultSecret(String vaultSecret) {
        this.vaultSecret = vaultSecret;
    }

    
    /**
     *
     * @return envVar if value is not empty otherwise return vaultKey
     */
    public String getEnvVar() {
        return StringUtils.isEmpty(envVar) ? vaultKey : envVar;
    }

    public String getVaultKey() {
        return vaultKey;
    }

    public String getVaultSecret() {
        return vaultSecret;
    }
    
    public boolean hasSecret() {
    	if (null !=vaultSecret) {
    		return true;
    	}
    	return false;
    }
    

    @Extension
    public static final class DescriptorImpl
        extends Descriptor<VaultSecretValue> {

        @Override
        public String getDisplayName() {
            return "Environment variable/vault secret value pair";
        }
    }

}
