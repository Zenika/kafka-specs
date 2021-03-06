/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.streamthoughts.kafka.specs;

import io.streamthoughts.kafka.specs.acl.AclGroupPolicy;
import io.streamthoughts.kafka.specs.acl.AclUserPolicy;
import io.streamthoughts.kafka.specs.resources.ConfigValue;
import io.streamthoughts.kafka.specs.resources.TopicResource;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

import static io.streamthoughts.kafka.specs.resources.Named.keyByName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class YAMLClusterSpecReaderTest {

    private static final TopicResource TOPIC_P1 = new TopicResource("my-topic-p1", 1, (short)1);

    private static final TopicResource TOPIC_P2 = new TopicResource("my-topic-p2", 2, (short)1)
            .addConfigValue(new ConfigValue(TopicConfig.RETENTION_MS_CONFIG, "10000", false));

    private static final TopicResource TOPIC_P3 = new TopicResource("my-topic-p3", null, null);

    private final YAMLClusterSpecReader reader = new YAMLClusterSpecReader();

    @Test
    public void should_fail_given_empty_file_using_current_version() {
        final InvalidSpecificationException e = Assertions.assertThrows(InvalidSpecificationException.class, () -> {
            readTestSample(reader, "");
        });
        assertEquals("Empty specification file", e.getLocalizedMessage());
    }

    @Test
    public void should_read_specification_given_empty_file_with_current_version() {
        final ClusterSpec specification = readTestSample(reader, "version: " + YAMLClusterSpecReader.CURRENT_VERSION.version());
        assertNotNull(specification);
    }

    @Test
    public void should_read_multiple_acl_groups_given_valid_YAML() {
        final ClusterSpec specification = reader.read(getResourceAsStream("test-acls-groups.yaml"));
        assertNotNull(specification);

        final Map<String, AclGroupPolicy> policies = keyByName(specification.getAcls().get().getAclGroupPolicies());
        assertEquals(2, policies.size());
        AclGroupPolicy groupOne = policies.get("group_one");
        assertNotNull(groupOne);

        assertEquals("group_one", groupOne.name());
        assertEquals("/([.-])*/", groupOne.permission().resource().pattern());
        assertEquals(4, groupOne.permission().operations().size());

        AclGroupPolicy groupTwo = policies.get("group_two");
        assertNotNull(groupTwo);
        assertEquals("group_two", groupTwo.name());
        assertEquals("/public-([.-])*/", groupTwo.permission().resource().pattern());
        assertEquals(2, groupTwo.permission().operations().size());
    }

    @Test
    public void should_read_multiple_acl_access_given_valid_YAML() {
        final ClusterSpec specification = reader.read(getResourceAsStream("test-acls-access.yaml"));
        assertNotNull(specification);
        Collection<AclUserPolicy> policies = specification.getAcls().get().getAclUsersPolicies();
        assertEquals(2, policies.size());
    }

    @Test
    public void should_read_multiple_topics_given_valid_YAML() {
        final ClusterSpec specification = reader.read(getResourceAsStream("test-topics.yaml"));
        assertNotNull(specification);
        Collection<TopicResource> topics = specification.getTopics();
        assertNotNull(topics);
        assertEquals(3, topics.size());
        assertTrue(topics.contains(TOPIC_P1), "should contain: " + TOPIC_P1);
        assertTrue(topics.contains(TOPIC_P2), "should contain: " + TOPIC_P2);
        assertTrue(topics.contains(TOPIC_P3), "should contain: " + TOPIC_P3);
    }

    private static InputStream getResourceAsStream(final String resource) {
        return YAMLClusterSpecReaderTest.class.getClassLoader().getResourceAsStream(resource);
    }

    private ClusterSpec readTestSample(final YAMLClusterSpecReader reader, final String content) {
        return reader.read(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}