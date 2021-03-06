package com.cbec.b2b.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cbec.b2b.entity.MsgResponse;
import com.cbec.b2b.entity.menu.Menu;
import com.cbec.b2b.entity.menu.MenuChildren;
import com.cbec.b2b.entity.message.MessageCountEntity;
import com.cbec.b2b.entity.message.MessageEntity;
import com.cbec.b2b.entity.register.RegisterStepTwo;
import com.cbec.b2b.entity.register.UserStatus;
import com.cbec.b2b.entity.response.CurrentUser;
import com.cbec.b2b.entity.response.LoginResponseEntity;
import com.cbec.b2b.entity.user.User;

@Service
public interface IUserService {
	LoginResponseEntity validate(String account,String password);
	CurrentUser getUser(String account);
	Boolean isAuth(String account,String url);
	List<Menu> getMenuTop(String account);
	List<MenuChildren> getMenuChildren(String account,String menuid);
	List<MessageEntity> getMessage(String account);
	MessageCountEntity getMessageCount(String account);
	String updateMessage(String account,String type);
	MsgResponse registerSubmit(String mail,String password,String type);
    String registerCode(String mail);
    String registerInfoUpload(RegisterStepTwo bean);
    UserStatus registerStatus(String account);
    String registerCheck(String account,String check);
    List<User> getPageUser(User user);
}
