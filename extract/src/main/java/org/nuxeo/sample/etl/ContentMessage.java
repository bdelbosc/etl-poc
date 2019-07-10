package org.nuxeo.sample.etl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.nuxeo.lib.stream.pattern.Message;

public class ContentMessage implements Message {
    static final long serialVersionUID = 1L;

    protected String key;

    protected String payload;

    protected static final String PAYLOAD_TEMPLATE = "{\n" + //
            "\n" + //
            "    \"entity-type\": \"exportDocumentEntry\",\n" + //
            "    \"value\": {\n" + //
            "        \"properties\": {\n" + //
            "            \"dc:source\": \"dvrrhh\",\n" + //
            "            \"dc:title\": \"%s\",\n" + //
            "            \"seiri:id\": \"%s\",\n" + //
            "            \"seiri:status\": \"ACTIVE\",\n" + //
            "            \"businessCodesDocument:parentFolders\": \"%s,%s\"\n" + //
            "        },\n" + //
            "        \"parentRef\": null,\n" + //
            "        \"repository\": null,\n" + //
            "        \"uid\": \"%s\",\n" + //
            "        \"name\": \"%s\",\n" + //
            "        \"changeToken\": null,\n" + //
            "        \"path\": \"/default-domain/workspaces/foo/bar\",\n" + //
            "        \"digest\": \"48e950c9694d33d06d94583611a2a59b\",\n" + //
            "        \"length\": \"1376\",\n" + //
            "        \"mimeType\": \"text/plain\",\n" + //
            "        \"type\": \"pedp00001\"\n" + //
            "    }\n" + //
            "\n" + //
            "}";

    public ContentMessage(String key, String payload) {
        this.key = Objects.requireNonNull(key);
        this.payload = Objects.requireNonNull(payload);
    }

    public ContentMessage() {
    }

    public static ContentMessage random() {
        String randomKey = UUID.randomUUID().toString();
        String randomPayload = String.format(PAYLOAD_TEMPLATE, //
                RandomStringUtils.randomAlphabetic(12), // title
                randomKey, // id
                randomKey, randomKey, // parents
                randomKey, // uid
                RandomStringUtils.randomAlphabetic(8) // name
        );
        return new ContentMessage(randomKey, randomPayload);
    }

    public String getKey() {
        return key;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Record{" + "key='" + key + '\'' + ", payload='" + payload + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ContentMessage record = (ContentMessage) o;
        return Objects.equals(key, record.key) && Objects.equals(payload, record.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, payload);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(key);
        out.writeObject(payload);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.key = (String) in.readObject();
        this.payload = (String) in.readObject();
    }

    @Override
    public String getId() {
        return key;
    }
}
