#include <stdio.h>

int main(void)
{
    printf("HTTP Server Started\n");

    // Simulate WiFi connection
    printf("Connecting to WiFi...\n");

    // Delay simulation
    for(int i = 0; i < 100000000; i++);

    printf("Connected successfully!\n");

    // Simulated IP address
    printf("IP Address: 192.168.4.1\n");

    printf("Server Started\n");

    // Simulated webpage content
    printf("\n--- Bus Details Page ---\n");
    printf("Bus No: 21A\n");
    printf("Route: Gandhipuram → Sulur\n");
    printf("Arrival Time: 10:30 AM\n");

    return 0;
}
