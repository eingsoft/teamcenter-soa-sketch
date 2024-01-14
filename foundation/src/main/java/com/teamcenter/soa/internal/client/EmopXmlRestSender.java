package com.teamcenter.soa.internal.client;

public class EmopXmlRestSender extends XmlRestSender {
    /**
     *
     */
    private static final long serialVersionUID = 4555178949551378897L;

    public EmopXmlRestSender(XmlRestSender sender, SessionManager sm) {
        super(sender.connection, sm, sender.m_transport, sender.m_notifier);
    }

    @Override
    public Object invoke2(String paramString1, String paramString2, Object paramObject, String paramString3,
                          String paramString4) {
        return super.invoke2(paramString1, paramString2, paramObject, paramString3, paramString4);
    }

}