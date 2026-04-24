package com.ecommerce.productservice.inbox.entity;

import com.ecommerce.common.entity.BaseInbox;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "inbox")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Inbox extends BaseInbox {
}