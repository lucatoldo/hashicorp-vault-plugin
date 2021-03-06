package com.datapipe.jenkins.vault;

import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.rest.RestResponse;
import com.datapipe.jenkins.vault.configuration.VaultConfiguration;
import com.datapipe.jenkins.vault.credentials.VaultCredential;
import com.datapipe.jenkins.vault.exception.VaultPluginException;
import com.datapipe.jenkins.vault.model.VaultSecret;
import com.datapipe.jenkins.vault.model.VaultSecretValue;
import hudson.EnvVars;
import hudson.model.Build;
import hudson.model.Run;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import jenkins.tasks.SimpleBuildWrapper;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VaultBuildWrapperTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testWrite() throws IOException, InterruptedException {
		String path = "are/existing";
		TestWrapperWrite wrapper = new TestWrapperWrite(writeSecrets(path));
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream logger = new PrintStream(baos);
		SimpleBuildWrapper.Context context = null;
		Run<?, ?> build = mock(Build.class);
		when(build.getParent()).thenReturn(null);
		EnvVars envVars = mock(EnvVars.class);
		when(envVars.expand(path)).thenReturn(path);

		wrapper.run(context, build, envVars, logger);

		try { // now we expect the exception to raise
			wrapper.vaultConfig.setFailIfNotFound(true);
			wrapper.run(context, build, envVars, logger);
		} catch (VaultPluginException e) {
			assertThat(e.getMessage(), is("Vault credentials not found for 'not/existing'"));
		}

		wrapper.verifyCalls();
		assertThat(new String(baos.toByteArray(), StandardCharsets.UTF_8),
				containsString("Vault credentials not found for 'not/existing'"));
	}

	@Test
	public void testWithNonExistingPath() throws IOException, InterruptedException {
		String path = "not/existing";
		TestWrapper wrapper = new TestWrapper(standardSecrets(path));
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream logger = new PrintStream(baos);
		SimpleBuildWrapper.Context context = null;
		Run<?, ?> build = mock(Build.class);
		when(build.getParent()).thenReturn(null);
		EnvVars envVars = mock(EnvVars.class);
		when(envVars.expand(path)).thenReturn(path);

		wrapper.run(context, build, envVars, logger);

		try { // now we expect the exception to raise
			wrapper.vaultConfig.setFailIfNotFound(true);
			wrapper.run(context, build, envVars, logger);
		} catch (VaultPluginException e) {
			assertThat(e.getMessage(), is("Vault credentials not found for 'not/existing'"));
		}

		wrapper.verifyCalls();
		assertThat(new String(baos.toByteArray(), StandardCharsets.UTF_8),
				containsString("Vault credentials not found for 'not/existing'"));
	}

	private List<VaultSecret> writeSecrets(String path) {
		List<VaultSecret> secrets = new ArrayList<>();
		VaultSecretValue secretValue = new VaultSecretValue("envVar1", "key1");
		secretValue.setVaultSecret("secret1");
		List<VaultSecretValue> secretValues = new ArrayList<>();
		secretValues.add(secretValue);
		VaultSecret secret = new VaultSecret(path, secretValues);
		secret.setEngineVersion(2);
		secrets.add(secret);
		return secrets;
	}
	
	
	private List<VaultSecret> standardSecrets(String path) {
		List<VaultSecret> secrets = new ArrayList<>();
		VaultSecretValue secretValue = new VaultSecretValue("envVar1", "key1");
		List<VaultSecretValue> secretValues = new ArrayList<>();
		secretValues.add(secretValue);
		VaultSecret secret = new VaultSecret(path, secretValues);
		secret.setEngineVersion(2);
		secrets.add(secret);
		return secrets;
	}

	@NotNull
	private LogicalResponse getNotFoundResponse() {
		LogicalResponse resp = mock(LogicalResponse.class);
		RestResponse rest = mock(RestResponse.class);
		when(resp.getData()).thenReturn(new HashMap<>());
		when(resp.getRestResponse()).thenReturn(rest);
		when(rest.getStatus()).thenReturn(404);
		return resp;
	}

	@NotNull
	private LogicalResponse writeResponse() {
		LogicalResponse resp = mock(LogicalResponse.class);
		RestResponse rest = mock(RestResponse.class);
		
		HashMap<String, String> response  = new HashMap<String, String>() {{
		    put("key1", "value1");
		}};
		
		when(resp.getData()).thenReturn(response);
		when(resp.getRestResponse()).thenReturn(rest);
		when(rest.getStatus()).thenReturn(200);
		return resp;
	}
	
	class TestWrapperWrite extends VaultBuildWrapper {

		VaultAccessor mockAccessor;
		VaultConfiguration vaultConfig = new VaultConfiguration();

		public TestWrapperWrite(List<VaultSecret> vaultSecrets) {
			super(vaultSecrets);
			vaultConfig.setVaultUrl("testmock");
			vaultConfig.setVaultCredentialId("credId");
			vaultConfig.setFailIfNotFound(false);
			mockAccessor = mock(VaultAccessor.class);
			doReturn(mockAccessor).when(mockAccessor).init();
			LogicalResponse response = writeResponse();
			when(mockAccessor.write(any(String.class), any(HashMap.class), any(Integer.class))).thenReturn(response);
			setVaultAccessor(mockAccessor);
			setConfiguration(vaultConfig);
		}
		
		public void run(Context context, Run build, EnvVars envVars, PrintStream logger) {
			this.logger = logger;
			provideEnvironmentVariablesFromVault(context, build, envVars);
		}

		@Override
		protected VaultCredential retrieveVaultCredentials(Run build) {
			return null;
		}

		public void verifyCalls() {
			verify(mockAccessor, times(2)).init();
			verify(mockAccessor, times(2)).read("not/existing", 2);
		}

	}

	class TestWrapper extends VaultBuildWrapper {

		VaultAccessor mockAccessor;
		VaultConfiguration vaultConfig = new VaultConfiguration();

		public TestWrapper(List<VaultSecret> vaultSecrets) {
			super(vaultSecrets);

			vaultConfig.setVaultUrl("testmock");
			vaultConfig.setVaultCredentialId("credId");
			vaultConfig.setFailIfNotFound(false);
			mockAccessor = mock(VaultAccessor.class);
			doReturn(mockAccessor).when(mockAccessor).init();
			LogicalResponse response = getNotFoundResponse();
			when(mockAccessor.read("not/existing", 2)).thenReturn(response);
			setVaultAccessor(mockAccessor);
			setConfiguration(vaultConfig);
		}

		public void run(Context context, Run build, EnvVars envVars, PrintStream logger) {
			this.logger = logger;
			provideEnvironmentVariablesFromVault(context, build, envVars);
		}

		@Override
		protected VaultCredential retrieveVaultCredentials(Run build) {
			return null;
		}

		public void verifyCalls() {
			verify(mockAccessor, times(2)).init();
			verify(mockAccessor, times(2)).read("not/existing", 2);
		}
	}

}
