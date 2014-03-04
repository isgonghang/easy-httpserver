package org.eh.core.http;

import java.io.IOException;
import java.io.OutputStream;

import org.eh.core.common.Constants;
import org.eh.core.model.ResultInfo;
import org.eh.core.util.StringUtil;
import org.eh.core.web.controller.Controller;
import org.eh.core.web.view.ViewHandler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * 处理Http请求
 * @author guojing
 * @date 2014-3-3
 */
@SuppressWarnings("restriction")
public class EHHttpHandler implements HttpHandler {

	public void handle(HttpExchange httpExchange) throws IOException {
		try {
			String path = httpExchange.getRequestURI().getPath();
			System.out.println("Request path:" + path);
			// 调用对应处理程序controller
			ResultInfo resultInfo = invokController(httpExchange);

			// 返回404
			if (resultInfo == null || StringUtil.isEmpty(resultInfo.getView())) {
				responseToClient(httpExchange, 404, null);
				return;
			}

			String viewPath = resultInfo.getView();
			// 重定向
			if (viewPath.startsWith("redirect:")) {
				String redirectUrl = viewPath.replace("redirect:", "");
				responseToClient(httpExchange, 302, redirectUrl);
				return;
			} else { // 解析对应view并返回
				String content = invokViewHandler(resultInfo);
				if (content == null) {
					content = "";
				}
				responseToClient(httpExchange, 200, content);
				return;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 响应请求
	 * @param httpExchange 请求-响应的封装
	 * @param code 返回状态码
	 * @param msg 返回信息
	 * @throws IOException
	 */
	private void responseToClient(HttpExchange httpExchange, Integer code, String msg)
			throws IOException {
		switch (code) {
		case 200: { // 成功
			byte[] bytes = msg.getBytes();
			httpExchange.sendResponseHeaders(code, bytes.length);
			OutputStream out = httpExchange.getResponseBody();
			out.write(bytes);
			out.flush();
		}
			break;
		case 302: { // 跳转
			Headers headers = httpExchange.getResponseHeaders();
			headers.add("Location", msg);
			httpExchange.sendResponseHeaders(code, 0);
		}
			break;
		case 404: { // 错误
			byte[] bytes = msg.getBytes();
			httpExchange.sendResponseHeaders(code, bytes.length);
		}
			break;
		default:
			break;
		}
	}

	private ResultInfo invokController(HttpExchange httpExchange) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		String path = httpExchange.getRequestURI().getPath();
		
		String classPath = Constants.UrlClassMap.get(path.substring(0, path.lastIndexOf(".")));
		if (classPath == null || classPath.length() == 0) {
			return null;
		}
		Class controllerClass = Class.forName(classPath);
		Controller controller = (Controller) controllerClass.newInstance();

		// 解析参数

		return controller.process(null);
	}

	private String invokViewHandler(ResultInfo resultInfo) {
		ViewHandler viewHandler = new ViewHandler();
		return viewHandler.processView(resultInfo);
	}
}