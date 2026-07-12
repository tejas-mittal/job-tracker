import os
import re
import shutil
from pathlib import Path

monolith_dir = Path("monolith-service/src/main/java/com/jobtracker/monolith")
events_dir = monolith_dir / "events"
events_dir.mkdir(parents=True, exist_ok=True)

# 1. Move all *Event.java files to events directory
event_classes = set()
for p in monolith_dir.rglob("*.java"):
    if p.parent == events_dir:
        continue
    if p.name.endswith("Event.java"):
        # move it
        dest = events_dir / p.name
        if dest.exists():
            p.unlink() # duplicate
        else:
            shutil.move(str(p), str(dest))
        event_classes.add(p.stem)

# 2. Update package and imports for events
for p in events_dir.glob("*.java"):
    content = p.read_text(encoding="utf-8")
    content = re.sub(r'package com\.jobtracker\.monolith\..+?;', 'package com.jobtracker.monolith.events;', content)
    # Remove any imports to other events since they are in the same package
    content = re.sub(r'import com\.jobtracker\.monolith\..+?Event;', '', content)
    p.write_text(content, encoding="utf-8")

# 3. Delete RabbitMQ configs
for p in monolith_dir.rglob("RabbitMQConfig.java"):
    p.unlink()
for p in monolith_dir.rglob("ProcessedEvent.java"): # Sometimes used for idempotency
    p.unlink()

# 4. Process all java files to fix imports, RabbitTemplate, and RabbitListeners
for p in monolith_dir.rglob("*.java"):
    if not p.is_file(): continue
    content = p.read_text(encoding="utf-8")
    original_content = content
    
    # Replace event imports
    for event_class in event_classes:
        content = re.sub(rf'import com\.jobtracker\.monolith\..+?\.{event_class};', f'import com.jobtracker.monolith.events.{event_class};', content)

    # Spring Events
    content = content.replace('import org.springframework.amqp.rabbit.core.RabbitTemplate;', 'import org.springframework.context.ApplicationEventPublisher;')
    content = content.replace('import org.springframework.amqp.rabbit.annotation.RabbitListener;', 'import org.springframework.context.event.EventListener;')
    
    # Remove AMQP imports
    content = re.sub(r'import org\.springframework\.amqp\..+?;\n', '', content)
    content = re.sub(r'import com\.rabbitmq\..+?;\n', '', content)

    # Publisher replacement
    content = content.replace('RabbitTemplate rabbitTemplate', 'ApplicationEventPublisher applicationEventPublisher')
    content = content.replace('private final RabbitTemplate rabbitTemplate;', 'private final ApplicationEventPublisher applicationEventPublisher;')
    content = content.replace('rabbitTemplate.convertAndSend', 'applicationEventPublisher.publishEvent')
    # Fix the method calls: rabbitTemplate.convertAndSend(exchange, routingKey, event) -> applicationEventPublisher.publishEvent(event)
    content = re.sub(r'applicationEventPublisher\.publishEvent\([^,]+,\s*[^,]+,\s*([a-zA-Z0-9_]+)\);', r'applicationEventPublisher.publishEvent(\1);', content)
    
    # Listener replacement
    # @RabbitListener(queues = RabbitMQConfig.STATUS_CHANGED_QUEUE)
    content = re.sub(r'@RabbitListener\([^)]+\)', '@EventListener', content)
    
    # Replace any leftover 'RabbitTemplate' if we missed it
    content = content.replace('RabbitTemplate', 'ApplicationEventPublisher')

    if content != original_content:
        p.write_text(content, encoding="utf-8")

print("Refactoring completed.")
