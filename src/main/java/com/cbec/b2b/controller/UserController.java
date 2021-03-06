package com.cbec.b2b.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cbec.b2b.api.UserApi;
import com.cbec.b2b.common.PageInfo;
import com.cbec.b2b.common.RedisUtil;
import com.cbec.b2b.common.TokenUtils;
import com.cbec.b2b.common.Util;
import com.cbec.b2b.entity.MsgResponse;
import com.cbec.b2b.entity.menu.Menu;
import com.cbec.b2b.entity.message.MessageCountEntity;
import com.cbec.b2b.entity.message.MessageEntity;
import com.cbec.b2b.entity.register.RegisterStepTwo;
import com.cbec.b2b.entity.register.UserStatus;
import com.cbec.b2b.entity.request.LoginEntity;
import com.cbec.b2b.entity.request.MessageRequest;
import com.cbec.b2b.entity.response.CurrentUser;
import com.cbec.b2b.entity.response.LoginResponseEntity;
import com.cbec.b2b.entity.user.User;

@RestController
@RequestMapping(value = "/llback/user")
public class UserController {

	@Autowired
	UserApi api;

	@Autowired
	private RedisUtil redisUtil;

	private static final Logger logger = LogManager.getLogger(UserController.class);

	@RequestMapping(value = "/validate")
	public String validate(@RequestBody LoginEntity loginEntity, HttpServletResponse res) {
		String pwd = loginEntity.getPassword();
		String pwdMd5 = "";
		if (pwd != null) {
			pwdMd5 = Util.getMD5(loginEntity.getPassword());
		}
		LoginResponseEntity response = api.validate(loginEntity.getUserName(), pwdMd5);
		String userId = loginEntity.getUserName();
		String token = TokenUtils.createToken(userId);
		boolean setResult = redisUtil.set(userId, token);
		response.setToken(token);
		if (setResult) {
			logger.info(String.format("保存token成功：[%s][%s]", userId, token));
		}
		Util.responseResultSuccess(res);
		return response.toString();
	}

	@RequestMapping(value = "/currentUser")
	public CurrentUser getUser(@RequestHeader(value = "userid") String userid, HttpServletResponse res) {
		CurrentUser response = api.getUser(userid);
		Util.responseResultSuccess(res);
		return response;
	}

	@RequestMapping(value = "/menu")
	public List<Menu> getMenu(@RequestHeader(value = "userid") String userid, HttpServletResponse res) {
		List<Menu> menuList = api.getMenu(userid);
		Util.responseResultSuccess(res);
		return menuList;
	}

	@RequestMapping(value = "/message/list")
	public List<MessageEntity> getMessage(@RequestHeader(value = "userid") String userid, HttpServletResponse res) {
		List<MessageEntity> response = api.getMessage(userid);
		Util.responseResultSuccess(res);
		return response;
	}

	@RequestMapping(value = "/message/count")
	public MessageCountEntity getMessageCount(@RequestHeader(value = "userid") String userid, HttpServletResponse res) {
		MessageCountEntity response = api.getMessageCount(userid);
		Util.responseResultSuccess(res);
		return response;
	}

	@RequestMapping(value = "/message/empty")
	public String updateMessage(@RequestHeader(value = "userid") String userid, @RequestBody MessageRequest requestBean,
			HttpServletResponse res) {
		String response = api.updateMessage(userid, requestBean.getType());
		Util.responseResultSuccess(res);
		return response;
	}

	@RequestMapping(value = "/register/submit")
	public MsgResponse registerSubmit(@RequestBody Map<String, String> request, HttpServletResponse res) {
		Util.responseResultSuccess(res);
		MsgResponse response = new MsgResponse();
		if (request == null || request.isEmpty()) {
			response.setMsg("无效请求");
			return response;
		}

		String mail = request.get("mail");
		String code = request.get("captcha");
		String pwd = request.get("password");
		String type = request.get("type");
		
		if (mail == null || code == null || pwd == null || type == null || "".equals(mail) || "".equals(code)
				|| "".equals(pwd) || "".equals(type) || (!"1".equals(type) && !"2".equals(type) && !"3".equals(type) && !"4".equals(type)) ) {
			response.setMsg("非法请求，参数有误.");
			return response;
		}
		if (!Util.checkEmail(mail)) {
			response.setMsg("邮件格式不正确.");
			return response;
		}
		
		if(pwd.length()<6) {
			response.setMsg("密码格式有误.");
			return response;
		}
		
		String key = mail + "_code";

		if (!redisUtil.isExistKey(key)) {
			response.setMsg("无效的验证码.");
			return response;
		}
		String redis_code = (String)redisUtil.get(key);
		if(!redis_code.equals(code)) {
			response.setMsg("验证码不正确.");
			return response;
		}

		return api.registerSubmit(mail, pwd, type);
	}

	@RequestMapping(value = "/register/code")
	public MsgResponse registerCode(@RequestBody Map<String, String> request, HttpServletResponse res) {
		Util.responseResultSuccess(res);
		MsgResponse response = new MsgResponse();
		if (request == null || request.isEmpty()) {
			response.setMsg("无效请求");
			return response;
		}
		String mail = request.get("mail");
		if (mail != null && !"".equals(mail)) {
			if (!Util.checkEmail(mail)) {
				response.setMsg("邮件格式不正确.");
				return response;
			}
			String redis_temp_code = mail + "_code_temp";
			if (redisUtil.isExistKey(redis_temp_code)) {
				Long leaveTime = (Long) redisUtil.getExpire(redis_temp_code);
				response.setMsg(leaveTime+"");
				response.setType("-1");
				return response;
			}
			redisUtil.set(redis_temp_code, mail, 60l);
			String code = api.registerCode(mail);
			redisUtil.set(mail + "_code", code, 10800l);
		} else {
			response.setMsg("非法的调用,账号为空.");
			return response;
		}
		response.setMsg("邮件已发送");
		response.setType("1");
		return response;
	}
	
	@RequestMapping(value = "/register/upload")
	public MsgResponse registerInfoUpload(@RequestBody RegisterStepTwo request,@RequestHeader(value = "userid") String userid, HttpServletResponse res) {
		Util.responseResultSuccess(res);
		request.setUserName(userid);
		MsgResponse response = new MsgResponse();
		response.setMsg(api.registerInfoUpload(request));
		response.setType("1");
		return response;
	}
	
	@RequestMapping(value = "/register/status")
	public UserStatus registerStatus(@RequestHeader(value = "userid") String userid, HttpServletResponse res) {
		Util.responseResultSuccess(res);
		return api.registerStatus(userid);
	}
	
	@RequestMapping(value = "/register/check")
	public MsgResponse registerCheck(@RequestBody Map<String,String> request, HttpServletResponse res) {
		Util.responseResultSuccess(res);
		String account = request.get("account");
		String check = request.get("check");
		MsgResponse response = new MsgResponse();
		response.setMsg(api.registerCheck(account,check));
		return response;
	}
	
	@RequestMapping(value = "/pagelist")
	public PageInfo<User> getPageUser(@RequestBody User request, HttpServletResponse res) {
		Util.responseResultSuccess(res);
		return api.getPageUser(request);
	}
	 
}
