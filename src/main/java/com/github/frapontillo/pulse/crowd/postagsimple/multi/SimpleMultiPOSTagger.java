/*
 * Copyright 2015 Francesco Pontillo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.frapontillo.pulse.crowd.postagsimple.multi;

import com.github.frapontillo.pulse.crowd.data.entity.Message;
import com.github.frapontillo.pulse.crowd.data.entity.Token;
import com.github.frapontillo.pulse.crowd.postagsimple.ISimplePOSTaggerOperator;
import com.github.frapontillo.pulse.spi.IPlugin;
import com.github.frapontillo.pulse.spi.ISingleablePlugin;
import com.github.frapontillo.pulse.spi.PluginProvider;
import com.github.frapontillo.pulse.spi.VoidConfig;
import com.github.frapontillo.pulse.util.PulseLogger;
import org.apache.logging.log4j.Logger;
import rx.Observable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Simple POS tagger that relies on external simple POS taggers based on the language they support.
 * For example, an English message will be tagged using the "simplepostagger-en" tagger, if any.
 *
 * @author Francesco Pontillo
 */
public class SimpleMultiPOSTagger extends IPlugin<Message, Message, VoidConfig> {
    public static final String PLUGIN_NAME = "simplepostagger-multi";
    private final Set<String> notFoundPosTaggers = new HashSet<>();
    private final static Logger logger = PulseLogger.getLogger(SimpleMultiPOSTagger.class);

    @Override public String getName() {
        return PLUGIN_NAME;
    }

    @Override public VoidConfig getNewParameter() {
        return new VoidConfig();
    }

    @Override public Observable.Operator<Message, Message> getOperator(VoidConfig parameters) {
        SimpleMultiPOSTagger actualTagger = this;
        return new ISimplePOSTaggerOperator(this) {
            @Override public List<Token> posTagMessageTokens(Message message) {
                return actualTagger.simplePosTagMessageTokens(message);
            }
        };
    }

    private List<Token> simplePosTagMessageTokens(Message message) {
        String language = message.getLanguage();
        if ((message.getTokens() == null) || (notFoundPosTaggers.contains(language))) {
            return message.getTokens();
        }

        IPlugin actualTagger = null;
        try {
            actualTagger = PluginProvider.getPlugin("simplepostagger-" + language).getInstance();
        } catch (ClassNotFoundException e) {
            logger.warn(
                    "Could not find a Simple POS Tagger implementation for the language \"{}\".",
                    language);
            notFoundPosTaggers.add(language);
        }
        if (actualTagger != null && actualTagger instanceof ISingleablePlugin) {
            return ((ISingleablePlugin<Message, VoidConfig>) actualTagger)
                    .singleItemProcess(message).getTokens();
        }
        return message.getTokens();
    }
}
