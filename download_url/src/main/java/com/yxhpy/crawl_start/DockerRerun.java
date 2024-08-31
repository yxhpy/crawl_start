package com.yxhpy.crawl_start;

import com.yxhpy.crawl_start.utils.GanymedExample;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DockerRerun {
    public static void main(String[] args) throws Exception {
        List<String> list = Arrays.asList(
                "192.168.3.64"
//                "192.168.3.73",
//                "192.168.3.57",
//                "192.168.3.75"
        );
        String username = "root";
        String password = "520612";
        for (String host : list) {
            String result = GanymedExample.executeCommand(host, username, password, "docker ps -a");
            String taskIds = Arrays.stream(result.split("\n"))
                    .skip(1)
                    .map(i -> i.split("\\s+")[0])
                    .collect(Collectors.joining(" "));
            GanymedExample.executeCommand(host, username, password, String.format("docker stop %s", taskIds));
            GanymedExample.executeCommand(host, username, password, String.format("docker rm %s", taskIds));
            String s = GanymedExample.executeCommand(host, username, password, "docker pull yxhpy520/crawl_start:latest");
            System.out.println(s);
            GanymedExample.executeCommand(host, username, password, "docker run -d yxhpy520/crawl_start:latest");
        }
    }
}
