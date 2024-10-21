package dev.flyfish.boot.cas.validator;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;

@ToString
public class ServiceTicketValidator {
    @Getter
    @Setter
    private String casValidateUrl;
    @Getter
    @Setter
    private String proxyCallbackUrl;
    private String st;
    @Setter
    private String service;
    @Getter
    private String pgtIou;
    @Getter
    private String user;
    @Getter
    private String errorCode;
    @Getter
    private String errorMessage;
    private String entireResponse;
    private String ss;
    @Setter
    @Getter
    private boolean renew = false;
    private boolean attemptedAuthentication;
    private boolean successfulAuthentication;

    public ServiceTicketValidator() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        ServiceTicketValidator sv = new ServiceTicketValidator();
        sv.setCasValidateUrl("https://portal1.wss.yale.edu/cas/serviceValidate");
        sv.setProxyCallbackUrl("https://portal1.wss.yale.edu/casProxy/receptor");
        sv.setService(args[0]);
        sv.setServiceTicket(args[1]);
        sv.validate();
        System.out.println(sv.getResponse());
        System.out.println();
        if (sv.isAuthenticationSuccessful()) {
            System.out.println("user: " + sv.getUser());
            System.out.println("pgtIou: " + sv.getPgtIou());
        } else {
            System.out.println("error code: " + sv.getErrorCode());
            System.out.println("error message: " + sv.getErrorMessage());
        }

    }

    public void setServiceTicket(String x) {
        this.st = x;
    }

    public boolean isAuthenticationSuccessful() {
        return this.successfulAuthentication;
    }

    public String getResponse() {
        return this.entireResponse;
    }

    public void validate() throws IOException, SAXException, ParserConfigurationException {
        if (this.casValidateUrl != null && this.st != null) {
            this.clear();
            this.attemptedAuthentication = true;
            StringBuilder sb = new StringBuilder();
            sb.append(this.casValidateUrl);
            if (this.casValidateUrl.indexOf(63) == -1) {
                sb.append('?');
            } else {
                sb.append('&');
            }

            sb.append("service=").append(this.service).append("&ticket=").append(this.st);
            if (this.proxyCallbackUrl != null) {
                sb.append("&pgtUrl=").append(this.proxyCallbackUrl);
            }

            if (this.renew) {
                sb.append("&renew=true");
            }

            String url = sb.toString();
            this.ss = url;
            String response = SecureURL.retrieve(url);
            this.entireResponse = response;
            if (response != null) {
                XMLReader r = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
                r.setFeature("http://xml.org/sax/features/namespaces", false);
                r.setContentHandler(this.newHandler());
                r.parse(new InputSource(new StringReader(response)));
            }

        } else {
            throw new IllegalStateException("must set validation URL and ticket");
        }
    }

    protected DefaultHandler newHandler() {
        return new Handler();
    }

    protected void clear() {
        this.user = this.pgtIou = this.errorMessage = null;
        this.attemptedAuthentication = false;
        this.successfulAuthentication = false;
    }

    protected class Handler extends DefaultHandler {
        protected static final String AUTHENTICATION_SUCCESS = "cas:authenticationSuccess";
        protected static final String AUTHENTICATION_FAILURE = "cas:authenticationFailure";
        protected static final String PROXY_GRANTING_TICKET = "cas:proxyGrantingTicket";
        protected static final String USER = "cas:user";
        protected StringBuffer currentText = new StringBuffer();
        protected boolean authenticationSuccess = false;
        protected boolean authenticationFailure = false;
        protected String netid;
        protected String pgtIou;
        protected String errorCode;
        protected String errorMessage;

        protected Handler() {
        }

        public void startElement(String ns, String ln, String qn, Attributes a) {
            this.currentText = new StringBuffer();
            if (qn.equals(AUTHENTICATION_SUCCESS)) {
                this.authenticationSuccess = true;
            } else if (qn.equals(AUTHENTICATION_FAILURE)) {
                this.authenticationFailure = true;
                this.errorCode = a.getValue("code");
                if (this.errorCode != null) {
                    this.errorCode = this.errorCode.trim();
                }
            }

        }

        public void characters(char[] ch, int start, int length) {
            this.currentText.append(ch, start, length);
        }

        public void endElement(String ns, String ln, String qn) throws SAXException {
            if (this.authenticationSuccess) {
                if (qn.equals(USER)) {
                    ServiceTicketValidator.this.user = this.currentText.toString().trim();
                }

                if (qn.equals(PROXY_GRANTING_TICKET)) {
                    this.pgtIou = this.currentText.toString().trim();
                }
            } else if (this.authenticationFailure && qn.equals(AUTHENTICATION_FAILURE)) {
                this.errorMessage = this.currentText.toString().trim();
            }

        }

        public void endDocument() throws SAXException {
            if (this.authenticationSuccess) {
                ServiceTicketValidator.this.user = ServiceTicketValidator.this.user;
                ServiceTicketValidator.this.pgtIou = this.pgtIou;
                ServiceTicketValidator.this.successfulAuthentication = true;
            } else {
                if (!this.authenticationFailure) {
                    throw new SAXException("no indication of success or failure from CAS");
                }

                ServiceTicketValidator.this.errorMessage = this.errorMessage;
                ServiceTicketValidator.this.errorCode = this.errorCode;
                ServiceTicketValidator.this.successfulAuthentication = false;
            }

        }
    }
}
