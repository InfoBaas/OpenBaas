package infosistema.openbaas.comunication.connector;

import infosistema.openbaas.comunication.message.Message;

public interface IConnector {
	
	public boolean sendMessage(Message message);

}
