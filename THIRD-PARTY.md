# Third-party software and licences

This prototype uses the following open-source software, each under its own licence.
No code from any UI template (including youlai's vue3-element-admin) is copied into this project;
the interface only follows a common sidebar-dashboard layout idiom.

| Component | Licence |
|---|---|
| Vue 3, Vite, Element Plus, Pinia, axios, vue-router, vue-i18n | MIT |
| Spring Boot (incl. starters) | Apache-2.0 |
| Flyway | Apache-2.0 |
| PostgreSQL server | PostgreSQL License |
| PostgreSQL JDBC driver | BSD-2-Clause |
| hey (load generator, used for measurement) | Apache-2.0 |
| pgbench (ships with PostgreSQL) | PostgreSQL License |

Layout reference: youlai organization, *vue3-element-admin* (MIT, https://github.com/youlaitech/vue3-element-admin).

## The University of Hong Kong visual identity

The interface carries the University's shield and wordmark (`frontend/src/assets/hku-*.svg`,
`frontend/public/favicon.svg`) and uses the HKU Green (#024638) and the associated gold
(#b49764) from the University's visual identity. Those marks are the property of The
University of Hong Kong. They are reproduced here to identify a coursework prototype made
for the University, and no licence to them is granted by this repository. The artwork was
taken from the University's own website (hku.hk) and is unmodified; for the small sidebar
mark it is cropped to the shield alone, and the wordmark is dropped.
