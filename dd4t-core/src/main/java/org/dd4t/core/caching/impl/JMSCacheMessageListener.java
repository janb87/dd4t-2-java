package org.dd4t.core.caching.impl;

import com.tridion.cache.CacheEvent;
import org.dd4t.core.caching.CacheInvalidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.io.Serializable;

/**
 * @author Mihai Cadariu
 */
public class JMSCacheMessageListener implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(JMSCacheMessageListener.class);
    @Autowired
    protected CacheInvalidator cacheInvalidator;
    @Autowired
    private JMSCacheMonitor monitor;

    @Override
    public void onMessage(Message message) {
        CacheEvent event = getCacheEvent(message);
        if (event != null) {
            switch (event.getType()) {
                case CacheEvent.INVALIDATE:
                    LOG.debug("Invalidate " + event);
                    Serializable key = event.getKey();
                    cacheInvalidator.invalidate(key.toString());
                    break;

                case CacheEvent.FLUSH:
                    LOG.debug("Flush " + event);
                    monitor.setMQServerStatusUp();
                    break;
            }
        }
    }

    private CacheEvent getCacheEvent (Message message) {
        CacheEvent event = null;

        try {
            if (message instanceof ObjectMessage) {
                ObjectMessage objectMessage = (ObjectMessage) message;
                Serializable serializable = objectMessage.getObject();
                if (serializable instanceof CacheEvent) {
                    event = (CacheEvent) serializable;
                } else {
                    LOG.error("JMS message is not a com.tridion.cache.CacheEvent");
                }
            } else {
                LOG.error("Unknown message type received: " + message.getClass().getName());
            }
        } catch (JMSException jmse) {
            LOG.error("Cannot read JMS message", jmse);
        }

        return event;
    }

    /**
     * Set the cache agent.
     */
    public void setCacheInvalidator(CacheInvalidator cacheAgent) {
        cacheInvalidator = cacheAgent;
    }
}