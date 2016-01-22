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
package org.neo4j.kernel.impl.factory;

import java.net.URL;
import java.util.Map;

import org.neo4j.graphdb.DatabaseShutdownException;
import org.neo4j.graphdb.DependencyResolver;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.security.URLAccessValidationError;
import org.neo4j.kernel.AvailabilityGuard;
import org.neo4j.kernel.PropertyTracker;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.TransactionFailureException;
import org.neo4j.kernel.impl.query.QueryExecutionKernelException;
import org.neo4j.kernel.impl.query.QuerySession;
import org.neo4j.kernel.impl.store.StoreId;
import org.neo4j.kernel.lifecycle.LifecycleException;
import org.neo4j.logging.Logger;

/**
 * This implements the backend for the "classic" Core API - meaning the surface-layer-of-the-database, thread bound API. It's a thin veneer to wire the
 * various components the kernel and related utilities expose in a way that {@link GraphDatabaseFacade} likes.
 * @see org.neo4j.kernel.impl.factory.GraphDatabaseFacade.SPI
 */
class ClassicCoreSPI implements GraphDatabaseFacade.SPI
{
    private final PlatformModule platform;
    private final DataSourceModule dataSource;
    private final Logger msgLog;
    private final EditionModule edition;

    public ClassicCoreSPI( PlatformModule platform, DataSourceModule dataSource, Logger msgLog, EditionModule edition )
    {
        this.platform = platform;
        this.dataSource = dataSource;
        this.msgLog = msgLog;
        this.edition = edition;
    }

    @Override
    public boolean databaseIsAvailable( long timeout )
    {
        return platform.availabilityGuard.isAvailable( timeout );
    }

    @Override
    public Result executeQuery( String query, Map<String,Object> parameters, QuerySession querySession )
    {
        try
        {
            assertDatabaseAvailable();
            return dataSource.queryExecutor.get().executeQuery( query, parameters, querySession );
        }
        catch ( QueryExecutionKernelException e )
        {
            throw e.asUserException();
        }
    }

    @Override
    public DependencyResolver resolver()
    {
        return platform.dependencies;
    }

    @Override
    public void registerKernelEventHandler( KernelEventHandler handler )
    {
        dataSource.kernelEventHandlers.registerKernelEventHandler( handler );
    }

    @Override
    public void unregisterKernelEventHandler( KernelEventHandler handler )
    {
        dataSource.kernelEventHandlers.unregisterKernelEventHandler( handler );
    }

    @Override
    public <T> void registerTransactionEventHandler( TransactionEventHandler<T> handler )
    {
        dataSource.transactionEventHandlers.registerTransactionEventHandler( handler );
    }

    @Override
    public <T> void unregisterTransactionEventHandler( TransactionEventHandler<T> handler )
    {
        dataSource.transactionEventHandlers.unregisterTransactionEventHandler( handler );
    }

    @Override
    public StoreId storeId()
    {
        return dataSource.storeId.get();
    }

    @Override
    public String storeDir()
    {
        return platform.storeDir.getAbsolutePath();
    }

    @Override
    public void addNodePropertyTracker( PropertyTracker<Node> tracker )
    {
        dataSource.nodeManager.addNodePropertyTracker( tracker );
    }

    @Override
    public void removeNodePropertyTracker( PropertyTracker<Node> tracker )
    {
        dataSource.nodeManager.removeNodePropertyTracker( tracker );
    }

    @Override
    public void addRelationshipPropertyTracker( PropertyTracker<Relationship> tracker )
    {
        dataSource.nodeManager.addRelationshipPropertyTracker( tracker );
    }

    @Override
    public void removeRelationshipPropertyTracker( PropertyTracker<Relationship> tracker )
    {
        dataSource.nodeManager.removeRelationshipPropertyTracker( tracker );
    }

    @Override
    public URL validateURLAccess( URL url ) throws URLAccessValidationError
    {
        return platform.urlAccessRule.validate( platform.config, url );
    }

    @Override
    public String name()
    {
        return platform.databaseInfo.toString();
    }

    @Override
    public void shutdown()
    {
        try
        {
            msgLog.log( "Shutdown started" );
            platform.availabilityGuard.shutdown();
            platform.life.shutdown();
        }
        catch ( LifecycleException throwable )
        {
            msgLog.log( "Shutdown failed", throwable );
            throw throwable;
        }
    }

    @Override
    public KernelTransaction beginTransaction()
    {
        try
        {
            assertDatabaseAvailable();
            KernelTransaction kernelTx = dataSource.kernelAPI.get().newTransaction();
            kernelTx.registerCloseListener( (s) -> dataSource.threadToTransactionBridge.unbindTransactionFromCurrentThread() );
            dataSource.threadToTransactionBridge.bindTransactionToCurrentThread( kernelTx );
            return kernelTx;
        }
        catch ( TransactionFailureException e )
        {
            throw new org.neo4j.graphdb.TransactionFailureException( e.getMessage(), e );
        }
    }

    @Override
    public KernelTransaction currentTransaction()
    {
        assertDatabaseAvailable();
        KernelTransaction tx = dataSource.threadToTransactionBridge.getKernelTransactionBoundToThisThread( false );
        if( tx == null )
        {
            throw new NotInTransactionException();
        }
        return tx;
    }

    @Override
    public boolean isInOpenTransaction()
    {
        return dataSource.threadToTransactionBridge.hasTransaction();
    }

    @Override
    public Statement currentStatement()
    {
        return dataSource.threadToTransactionBridge.get();
    }

    private void assertDatabaseAvailable()
    {
        try
        {
            platform.availabilityGuard.await( edition.transactionStartTimeout );
        }
        catch ( AvailabilityGuard.UnavailableException e )
        {
            if( platform.availabilityGuard.isShutdown())
            {
                throw new DatabaseShutdownException();
            }
            throw new org.neo4j.graphdb.TransactionFailureException( e.getMessage() );
        }
    }
}