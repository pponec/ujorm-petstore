# Using the latest stable version for 2026
FROM postgres:18-alpine

# Create directory for custom configuration files
RUN mkdir -p /etc/postgresql

# Copy the minimalist configuration (assumes file exists in ./config/ folder)
COPY config/postgresql.conf /etc/postgresql/postgresql.conf

# Set permissions for the postgres user
RUN chown -R postgres:postgres /etc/postgresql

# Start Postgres using the custom configuration file
CMD ["postgres", "-c", "config_file=/etc/postgresql/postgresql.conf"]