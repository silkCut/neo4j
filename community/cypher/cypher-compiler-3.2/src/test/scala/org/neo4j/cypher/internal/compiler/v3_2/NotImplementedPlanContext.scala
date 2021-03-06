/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.v3_2

import org.neo4j.cypher.internal.compiler.v3_2.spi._
import org.neo4j.kernel.api.constraints.UniquenessConstraint
import org.neo4j.kernel.api.index.IndexDescriptor

class NotImplementedPlanContext extends PlanContext {
  override def getIndexRule(labelName: String, propertyKey: String): Option[IndexDescriptor] = ???

  override def hasIndexRule(labelName: String): Boolean = ???

  override def getUniqueIndexRule(labelName: String, propertyKey: String): Option[IndexDescriptor] = ???

  override def getUniquenessConstraint(labelName: String, propertyKey: String): Option[UniquenessConstraint] = ???

  override def hasPropertyExistenceConstraint(labelName: String, propertyKey: String): Boolean = ???

  override def checkNodeIndex(idxName: String): Unit = ???

  override def checkRelIndex(idxName: String): Unit = ???

  override def getOrCreateFromSchemaState[T](key: Any, f: => T): T = ???

  override def txIdProvider: () => Long = ???

  override def statistics: GraphStatistics = ???

  override def notificationLogger(): InternalNotificationLogger = ???

  override def procedureSignature(name: QualifiedName): ProcedureSignature = ???

  override def functionSignature(name: QualifiedName): Option[UserFunctionSignature] = ???

  override def getLabelName(id: Int): String = ???

  override def getOptLabelId(labelName: String): Option[Int] = ???

  override def getLabelId(labelName: String): Int = ???

  override def getPropertyKeyName(id: Int): String = ???

  override def getOptPropertyKeyId(propertyKeyName: String): Option[Int] = ???

  override def getPropertyKeyId(propertyKeyName: String): Int = ???

  override def getRelTypeName(id: Int): String = ???

  override def getOptRelTypeId(relType: String): Option[Int] = ???

  override def getRelTypeId(relType: String): Int = ???
}
