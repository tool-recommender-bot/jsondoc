package org.jsondoc.springmvc.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jsondoc.core.pojo.ApiDoc;
import org.jsondoc.core.pojo.ApiHeaderDoc;
import org.jsondoc.core.pojo.ApiMethodDoc;
import org.jsondoc.core.pojo.ApiParamDoc;
import org.jsondoc.core.pojo.ApiResponseObjectDoc;
import org.jsondoc.core.pojo.ApiVerb;
import org.jsondoc.core.util.AbstractJSONDocScanner;
import org.jsondoc.core.util.JSONDocScanner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;

public class SpringJSONDocScanner extends AbstractJSONDocScanner implements JSONDocScanner {

	@Override
	public ApiDoc mergeApiDoc(Class<?> controller, ApiDoc apiDoc) {
		return apiDoc;
	}

	/**
	 * This method merges the documentation built with the JSONDoc default scanner with data extracted from the
	 * Spring's annotations.
	 * Path is calculated as the concatenation between the RequestMapping "value" on the controller class
	 * and the RequestMapping "value" on the method signature. Eventually replaces the "path" value of the JSONDoc annotation.
	 * Verb follows the same approach as "path".
	 * Produces (consumes) are calculated by looking at the RequestMapping "produces" ("consumes") and in case they are redefined
	 * at method level, then those overwrite the previous ones.
	 * Headers follow the same approach as "produces" and "consumes".
	 * Response objects are calculated by the JSONDoc default scanner and then eventually modified in case the return object is a
	 * ResponseEntity class, in that case the "responseentity" string is removed from the final documentation because not meaningful
	 * for documentation users.
	 * Request body is calculated by the JSONDoc default scanner. No need for integration with Spring's RequestBody annotation. 
	 */
	@Override
	public ApiMethodDoc mergeApiMethodDoc(Method method, Class<?> controller, ApiMethodDoc apiMethodDoc) {
		apiMethodDoc.setPath(getPathFromSpringAnnotation(method, controller));
		apiMethodDoc.setVerb(getApiVerbFromSpringAnnotation(method, controller));
		apiMethodDoc.getProduces().addAll(getProducesFromSpringAnnotation(method, controller));
		apiMethodDoc.getConsumes().addAll(getConsumesFromSpringAnnotation(method, controller));
		apiMethodDoc.getHeaders().addAll(getHeadersFromSpringAnnotation(method, controller));
		apiMethodDoc.setResponse(getApiResponseObject(method, apiMethodDoc.getResponse()));
		return apiMethodDoc;
	}
	
	@Override
	public ApiParamDoc mergeApiPathParamDoc(Method method, int paramIndex, ApiParamDoc apiParamDoc) {
		Annotation[] parameterAnnotations = method.getParameterAnnotations()[paramIndex];
		
		for (Annotation annotation : parameterAnnotations) {
			if(annotation instanceof PathVariable) {
				PathVariable pathVariable = (PathVariable) annotation; 
				if(!pathVariable.value().isEmpty()) {
					apiParamDoc.setName(pathVariable.value());
				}
			}
		}
		return apiParamDoc;
	}
	
	@Override
	public ApiParamDoc mergeApiQueryParamDoc(Method method, int paramIndex, ApiParamDoc apiParamDoc) {
		Annotation[] parameterAnnotations = method.getParameterAnnotations()[paramIndex];
		
		for (Annotation annotation : parameterAnnotations) {
			if(annotation instanceof RequestParam) {
				RequestParam requestParam = (RequestParam) annotation; 
				if(!requestParam.value().isEmpty()) {
					apiParamDoc.setName(requestParam.value());
				}
				apiParamDoc.setRequired(String.valueOf(requestParam.required()));
				if(!requestParam.defaultValue().equals(ValueConstants.DEFAULT_NONE)) {
					apiParamDoc.setDefaultvalue(requestParam.defaultValue());
				}
				
			}
		}
		return apiParamDoc;
	}
	
	/**
	 * Gets the ApiResponseObjectDoc built by JSONDoc and checks if the first type corresponds to a ResponseEntity class. In that case removes the "responseentity"
	 * string from the final list because it's not important to the documentation user.
	 * @param method
	 * @param apiResponseObjectDoc
	 * @return
	 */
	private ApiResponseObjectDoc getApiResponseObject(Method method, ApiResponseObjectDoc apiResponseObjectDoc) {
		if(method.getReturnType().isAssignableFrom(ResponseEntity.class)) {
			apiResponseObjectDoc.getJsondocType().getType().remove(0);
		}
		
		return apiResponseObjectDoc;
	}
	
	private Set<ApiHeaderDoc> getHeadersFromSpringAnnotation(Method method, Class<?> controller) {
		Set<ApiHeaderDoc> headers = new LinkedHashSet<ApiHeaderDoc>();
		
		if(controller.isAnnotationPresent(RequestMapping.class)) {
			RequestMapping requestMapping = controller.getAnnotation(RequestMapping.class);
			List<String> headersStringList = Arrays.asList(requestMapping.headers());
			for (String header : headersStringList) {
				headers.add(new ApiHeaderDoc(header.split("=")[0], null));
			}
		}
		
		if(method.isAnnotationPresent(RequestMapping.class)) {
			RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
			if(requestMapping.headers().length > 0) {
				headers.clear();
				List<String> headersStringList = Arrays.asList(requestMapping.headers());
				for (String header : headersStringList) {
					headers.add(new ApiHeaderDoc(header.split("=")[0], null));
				}
			}
		}
		
		return headers;
	}
	
	/**
	 * From Spring's documentation: [produces is] supported at the type level as well as at the method level! 
	 * When used at the type level, all method-level mappings override this produces restriction.
	 * @param method
	 * @param controller
	 * @return
	 */
	private Set<String> getProducesFromSpringAnnotation(Method method, Class<?> controller) {
		Set<String> produces = new LinkedHashSet<String>();
		
		if(controller.isAnnotationPresent(RequestMapping.class)) {
			RequestMapping requestMapping = controller.getAnnotation(RequestMapping.class);
			if(requestMapping.produces().length > 0) {
				produces.addAll(Arrays.asList(requestMapping.produces()));
			}
		}
		
		if(method.isAnnotationPresent(RequestMapping.class)) {
			RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
			if(requestMapping.produces().length > 0) {
				produces.clear();
				produces.addAll(Arrays.asList(requestMapping.produces()));
			}
		}
		
		return produces;
	}
	
	private Set<String> getConsumesFromSpringAnnotation(Method method, Class<?> controller) {
		Set<String> consumes = new LinkedHashSet<String>();
		
		if(controller.isAnnotationPresent(RequestMapping.class)) {
			RequestMapping requestMapping = controller.getAnnotation(RequestMapping.class);
			if(requestMapping.consumes().length > 0) {
				consumes.addAll(Arrays.asList(requestMapping.consumes()));
			}
		}
		
		if(method.isAnnotationPresent(RequestMapping.class)) {
			RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
			if(requestMapping.consumes().length > 0) {
				consumes.clear();
				consumes.addAll(Arrays.asList(requestMapping.consumes()));
			}
		}
		
		return consumes;
	}
	
	private ApiVerb getApiVerbFromSpringAnnotation(Method method, Class<?> controller) {
		ApiVerb apiVerb = null;
		
		if(controller.isAnnotationPresent(RequestMapping.class)) {
			RequestMapping requestMapping = controller.getAnnotation(RequestMapping.class);
			if(requestMapping.method().length > 0) {
				apiVerb = ApiVerb.valueOf(requestMapping.method()[0].name());
			}
		}
		
		if(method.isAnnotationPresent(RequestMapping.class)) {
			RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
			if(requestMapping.method().length > 0) {
				apiVerb = ApiVerb.valueOf(requestMapping.method()[0].name());
			}
		}
		
		if(apiVerb == null) {
			apiVerb = ApiVerb.GET;
		}
		
		return apiVerb;
	}
	
	private String getPathFromSpringAnnotation(Method method, Class<?> controller) {
		StringBuffer pathStringBuffer = new StringBuffer();
		
		if(controller.isAnnotationPresent(RequestMapping.class)) {
			RequestMapping requestMapping = controller.getAnnotation(RequestMapping.class);
			if(requestMapping.value().length > 0) {
				pathStringBuffer.append(requestMapping.value()[0]);
			}
		}
		
		if(method.isAnnotationPresent(RequestMapping.class)) {
			RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
			if(requestMapping.value().length > 0) {
				pathStringBuffer.append(requestMapping.value()[0]);
			}
		}
		
		return pathStringBuffer.toString();
	}

}
