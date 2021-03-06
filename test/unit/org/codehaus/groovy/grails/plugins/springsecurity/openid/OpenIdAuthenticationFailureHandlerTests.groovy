/* Copyright 2006-2012 SpringSource.
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
package org.codehaus.groovy.grails.plugins.springsecurity.openid

import org.codehaus.groovy.grails.plugins.springsecurity.ReflectionUtils
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.AccountExpiredException
import org.springframework.security.core.context.SecurityContextHolder as SCH
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.openid.OpenIDAttribute
import org.springframework.security.openid.OpenIDAuthenticationStatus
import org.springframework.security.openid.OpenIDAuthenticationToken

/**
 * Unit tests for <code>OpenIdAuthenticationFailureHandler</code>.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class OpenIdAuthenticationFailureHandlerTests extends GroovyTestCase {

	private static final String OPENID_REDIRECT = '/login/openIdCreate'
	private static final String AJAX_REDIRECT = '/ajaxAuthenticationFailureUrl'
	private static final String STANDARD_REDIRECT = '/defaultFailureUrl'

	private _handler = new OpenIdAuthenticationFailureHandler()

	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() {
		super.setUp()
		ReflectionUtils.application = new FakeApplication()
		ReflectionUtils.setConfigProperty 'openid.registration.autocreate', true
		ReflectionUtils.setConfigProperty 'ajaxHeader', 'ajaxHeader'
		ReflectionUtils.setConfigProperty 'openid.registration.createAccountUri', OPENID_REDIRECT
		_handler.defaultFailureUrl = STANDARD_REDIRECT
		_handler.ajaxAuthenticationFailureUrl = AJAX_REDIRECT
	}

	void testOnAuthenticationFailure_NotUsernameNotFound() {
		def response = new MockHttpServletResponse()

		_handler.onAuthenticationFailure new MockHttpServletRequest(),
			response, new AccountExpiredException('expired')

		assertEquals STANDARD_REDIRECT, response.redirectedUrl
	}

	void testOnAuthenticationFailure_NotOpenId() {
		def response = new MockHttpServletResponse()

		_handler.onAuthenticationFailure new MockHttpServletRequest(),
			response, new UsernameNotFoundException('expired')

		assertEquals STANDARD_REDIRECT, response.redirectedUrl
	}

	void testOnAuthenticationFailure_NotOpenIdSuccess() {
		def e = new UsernameNotFoundException('expired')
		e.authentication = new OpenIDAuthenticationToken(OpenIDAuthenticationStatus.FAILURE, "", "", [])
		def response = new MockHttpServletResponse()

		_handler.onAuthenticationFailure new MockHttpServletRequest(), response, e

		assertEquals STANDARD_REDIRECT, response.redirectedUrl
	}

	void testOnAuthenticationFailure_OpenIdSuccess_NotAutocreate() {
		ReflectionUtils.setConfigProperty 'openid.registration.autocreate', false
		def e = new UsernameNotFoundException('expired')
		e.authentication = new OpenIDAuthenticationToken(OpenIDAuthenticationStatus.SUCCESS, "", "", [])
		def response = new MockHttpServletResponse()

		_handler.onAuthenticationFailure new MockHttpServletRequest(), response, e

		assertEquals STANDARD_REDIRECT, response.redirectedUrl
	}

	void testOnAuthenticationFailure_OpenIdSuccess_Autocreate() {
		def e = new UsernameNotFoundException('expired')
		String openId = 'http://foo.someopenid.com'
		e.authentication = new OpenIDAuthenticationToken(
				OpenIDAuthenticationStatus.SUCCESS, openId, '',
				[new OpenIDAttribute('email', 'type', ['foo@bar.com'])])
		def response = new MockHttpServletResponse()
		def request = new MockHttpServletRequest()

		_handler.onAuthenticationFailure request, response, e

		assertEquals OPENID_REDIRECT, response.redirectedUrl
		assertEquals openId, request.session.getAttribute(OpenIdAuthenticationFailureHandler.LAST_OPENID_USERNAME)
		def attributes = request.session.getAttribute(OpenIdAuthenticationFailureHandler.LAST_OPENID_ATTRIBUTES)
		assertEquals 1, attributes.size()
		assertEquals 'foo@bar.com', attributes[0].values[0]
	}

	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() {
		super.tearDown()
		SCH.context.authentication = null
		ReflectionUtils.application = null
		SpringSecurityUtils.resetSecurityConfig()
	}
}
