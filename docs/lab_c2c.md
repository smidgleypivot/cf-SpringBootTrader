# Container 2 Container Networking.

1. In Portfolio Service show that there is a public route to the application.
2. Remove this route ```cf unmap-route portfolio cfapps.lab01.pcf.pw -n portfolio-agile-toucan```
3. Bind the new route ```change the manifest file```
4. Rebuild the app ```gradle clean build && cf push```
5. Log into Service Registry and show that the address registered for portfolio service has changed
6. Attempt to use the application - you will not see any portfolio related data
7. Add network policy ```cf add-network-policy webtrader --destination-app portfolio```


