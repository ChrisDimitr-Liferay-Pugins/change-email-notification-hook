package com.liferay.listener;

import java.io.IOException;

import javax.mail.internet.InternetAddress;

import com.liferay.mail.service.MailServiceUtil;
import com.liferay.portal.ModelListenerException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.mail.MailMessage;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.model.BaseModelListener;
import com.liferay.portal.model.User;
import com.liferay.portal.service.UserLocalServiceUtil;

public class CustomUserListener extends BaseModelListener<User> {
	
	@Override
    public void onBeforeUpdate(User user) throws ModelListenerException {    	
    	User persistedUser = null;
    	
    	try {
			persistedUser = UserLocalServiceUtil.getUserById(user.getUserId());
		} catch (PortalException | SystemException e) {
			e.printStackTrace();
			
			return;
		}
    	
    	if ( !user.getEmailAddress().equals(persistedUser.getEmailAddress()) ) {
    		sendEmail(user, persistedUser);
    	}
    }
    
    private void sendEmail(User modelUser, User persistedUser) {       
        ClassLoader classLoader = getClass().getClassLoader();
        
        long companyId = persistedUser.getCompanyId();      
        
        try {
        	String fromName = PrefsPropsUtil.getString(companyId, "admin.email.from.name");
            String fromAddress = PrefsPropsUtil.getString(companyId, "admin.email.from.address");
            
        	String subject = StringUtil.read(classLoader, "com/liferay/listener/dependencies/user_email_changed_subject.tmpl");
        	String body = StringUtil.read(classLoader, "com/liferay/listener/dependencies/user_email_changed_body.tmpl");
       
        	body = StringUtil.replace(
        			body,
        			new String[] {
                        "[$TO_NAME$]",
                        "[$PREV_EMAIL$]",
                        "[$NEW_EMAIL$]"
        			},
        			new String[] {
        				persistedUser.getFullName(),
        				persistedUser.getEmailAddress(),
        				modelUser.getEmailAddress(),
        	});    
        
			InternetAddress from = new InternetAddress(fromAddress, fromName);
			InternetAddress to = new InternetAddress(modelUser.getEmailAddress(), persistedUser.getFullName());
			
			MailMessage mailMessage = new MailMessage(from, to, subject, body, true);
		       
	        MailServiceUtil.sendEmail(mailMessage);
		} catch (IOException | SystemException e) {
			e.printStackTrace();
		}
    }
}
