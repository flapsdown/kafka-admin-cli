package se.flapsdown.kafka.admin.cli.acl;

import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.*;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@CommandLine.Command(description = "List acls",  name = "ListAcls")
public class ListAcls implements Callable<Void> {

    @CommandLine.ParentCommand
    public Acls acls;

    @CommandLine.Option(names = {"-n", "--name"}, description = "Resource name (required)", required = true)
    private String name;

    @CommandLine.Option(names = {"-t", "--type"}, description = "Resource type [any,topic, group, cluster, transaction_id, delegation_token, unknown] (defaults to ANY)")
    private String type = "any";


    public static final AccessControlEntryFilter ANY =
            new AccessControlEntryFilter(null, null, AclOperation.ANY, AclPermissionType.ANY);

    @Override
    public Void call() {

        // Kafka 2.0
        //AclBindingFilter filter =
        //        new AclBindingFilter(new ResourcePatternFilter(ResourceType.TOPIC, name, PatternType.MATCH), ListAcls.ANY);


        List<AclBinding> collect = Arrays.asList(name.split(","))
                .stream()
                .map(s -> new AclBindingFilter(new ResourceFilter(ResourceType.fromString(type), s), ListAcls.ANY))
                .map(filter -> acls.cli.adminClient().describeAcls(filter))
                .map(result -> result.values())
                .map(collectionKafkaFuture -> waitForResponse(collectionKafkaFuture))
                .flatMap(aclBindings -> aclBindings.stream())
                .collect(Collectors.toList());

        acls.cli.print(collect);

        return null;
    }

    private Collection<AclBinding> waitForResponse(KafkaFuture<Collection<AclBinding>> collectionKafkaFuture) {
        try {
            return collectionKafkaFuture.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
}
