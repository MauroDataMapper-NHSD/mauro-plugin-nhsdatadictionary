/*
 * Copyright 2020-2025 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.nhs.datadictionary.publish.structure

import groovy.xml.MarkupBuilder
import uk.nhs.datadictionary.publish.PublishContext

interface HtmlAware {
    String generateHtml(PublishContext context)
}

interface HtmlBuilder {
    void buildHtml(PublishContext context, MarkupBuilder builder)
}

class HtmlConstants {
    static final CSS_TOPIC_BODY = "- topic/body body"
    static final CSS_TOPIC_TITLE = "title topictitle1"
    static final CSS_TOPIC_SHORTDESC = "- topic/shortdesc shortdesc"
    static final CSS_TOPIC_PARAGRAPH = "- topic/p p"
    static final CSS_TOPIC_DIV = "- topic/div div"
    static final CSS_TOPIC_UNORDERED_LIST = "- topic/ul ul"
    static final CSS_TOPIC_LIST_ITEM = "- topic/li li"

    static final CSS_TABLE = "table table-sm table-striped"
    static final CSS_TABLE_PRIMARY = "table-primary"
    static final CSS_DATA_SET_TABLE = "table table-sm table-striped table-bordered"
    static final CSS_TABLE_HTML_CONTAINER_SIMPLE = "simpletable-container"
    static final CSS_TABLE_HTML_CONTAINER = "table-container"
    static final CSS_TABLE_HTML_TOPIC_SIMPLETABLE = "- topic/simpletable simpletable"
    static final CSS_TABLE_HTML_TOPIC_TABLE = "- topic/table table table-striped table-bordered table-sm"
    static final CSS_TABLE_HEAD = "thead-light"
    static final CSS_TABLE_TOPIC_STHEAD = "- topic/sthead sthead"
    static final CSS_TABLE_TOPIC_HEAD = "- topic/thead thead"
    static final CSS_TABLE_TOPIC_BODY = "- topic/tbody tbody"

    static final CSS_TABLE_STROW = "- topic/strow strow"
    static final CSS_TABLE_STENTRY = "- topic/stentry stentry"
    static final CSS_TABLE_ROW = "- topic/row"
    static final CSS_TABLE_ENTRY = "- topic/entry entry"

    static final CSS_DIFF_NEW = "new"
    static final CSS_DIFF_DELETED = "deleted"

    static final CSS_HTML_ALIGN_CENTER = "align-center"

    static final CSS_INFO_MESSAGE_PARAGRAPH = "info-message"
}